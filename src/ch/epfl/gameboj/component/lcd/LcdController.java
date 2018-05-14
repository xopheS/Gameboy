package ch.epfl.gameboj.component.lcd;

import static ch.epfl.gameboj.component.lcd.LcdImage.BLANK_LCD_IMAGE;
import static ch.epfl.gameboj.component.lcd.LcdImageLine.BLANK_LCD_IMAGE_LINE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;

public final class LcdController implements Component, Clocked {

    private enum LCDReg implements Register {
        LCDC, STAT, SCY, SCX, LY, LYC, DMA, BGP, OBP0, OBP1, WY, WX
    }

    private enum LCDC implements Bit {
        BG, OBJ, OBJ_SIZE, BG_AREA, TILE_SOURCE, WIN, WIN_AREA, LCD_STATUS
    }

    private enum STAT implements Bit {
        MODE0, MODE1, LYC_EQ_LY, INT_MODE0, INT_MODE1, INT_MODE2, INT_LYC, UNUSED7
    }

    private enum DISPLAY_DATA {
        Y_COORD, X_COORD, TILE_INDEX, ATTRIBUTES
    }

    private enum ATTRIBUTES implements Bit {
        P_NUM0, P_NUM1, P_NUM2, VRAM_BANK, PALETTE, FLIP_H, FLIP_V, BEHIND_BG
    }

    private static final int TILE_SIZE = 8;
    // Background size: 256 x 256, 32 x 32 tiles
    private static final int BG_SIZE = 256;
    private static final int BG_TILE_SIZE = BG_SIZE / TILE_SIZE;
    // Resolution: 160 x 144, 20 x 18 tiles
    public static final int LCD_WIDTH = 160, LCD_HEIGHT = 144;
    private static final int WIN_SIZE = BG_SIZE, WIN_TILE_SIZE = BG_TILE_SIZE;
    private static final int SPRITE_XOFFSET = 8, SPRITE_YOFFSET = 16;
    // Max sprites: 40 per screen, 10 per line
    private static final int MAX_SPRITES = 10, OAM_SPRITES = 40;
    private static final int SPRITE_ATTR_BYTES = 4;
    private static final int BYTES_PER_TILE = 16;
    private static final int WX_OFFSET = 7;
    // Cycle durations
    private static final int MODE2_DURATION = 20, MODE3_DURATION = 43, MODE0_DURATION = 51;
    public static final int LINE_CYCLE_DURATION = MODE2_DURATION + MODE3_DURATION + MODE0_DURATION;
    public static final int IMAGE_CYCLE_DURATION = 154 * LINE_CYCLE_DURATION;
    private LcdImage displayedImage = BLANK_LCD_IMAGE;
    private final RamController videoRamController, oamRamController;
    private final Cpu cpu;
    private long nextNonIdleCycle = Long.MAX_VALUE;
    private int winY;
    private LcdImage.Builder nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
    private final DmaController dmaController = DmaController.getDmaController();
    private final RegisterFile<Register> lcdRegs = new RegisterFile<>(LCDReg.values());
    private long lcdOnCycle;

    // TODO can only access HRAM during quick copy

    /**
     * Construit un contrôleur LCD.
     * 
     * @param cpu
     *            le CPU avec lequel le contrôleur interagit
     */
    public LcdController(Cpu cpu) {
        this.cpu = Objects.requireNonNull(cpu);

        videoRamController = new RamController(new Ram(AddressMap.VRAM_SIZE), AddressMap.VRAM_START);
        oamRamController = new RamController(new Ram(AddressMap.OAM_SIZE), AddressMap.OAM_START);
    }

    public LcdImage currentImage() {
        return Objects.requireNonNull(displayedImage, "Displayed image cannot be null");
    }

    public LcdImage getBackground() {
        LcdImage.Builder backgroundBuilder = new LcdImage.Builder(BG_SIZE, BG_SIZE);
        for (int y = 0; y < BG_SIZE; ++y) {
            backgroundBuilder.setLine(y, computeBGLine(y));
        }
        return backgroundBuilder.build();
    }

    public LcdImage getWindow() {
        LcdImage.Builder windowBuilder = new LcdImage.Builder(WIN_SIZE, WIN_SIZE);
        for (int y = 0; y < WIN_SIZE; ++y) {
            windowBuilder.setLine(y, computeWinLine(0, WIN_SIZE));
        }
        return windowBuilder.build();
    }

    public LcdImage getSprites() { // TODO whole screen or just lcd?
        LcdImage.Builder spriteBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);

        int scy = lcdRegs.get(LCDReg.SCY);

        for (int y = 0; y < LCD_HEIGHT; ++y) {
            spriteBuilder.setLine(y, computeSpriteLine(spritesIntersectingLine(y), y));
        }
        return spriteBuilder.build();
    }

    @Override
    public void cycle(long cycle) {
        if (nextNonIdleCycle == Long.MAX_VALUE && isOn()) {
            turnOn(cycle);
        }

        if (dmaController.isActive()) {
            dmaController.copy();
        }

        // cycFromImg = Math.floorMod(cycFromPowOn, IMAGE_CYCLE_DURATION);
        // cycFromLn = Math.floorMod(cycFromImg, LINE_CYCLE_DURATION);

        // compareLYandLYC(); //TODO correct?

        long cycFromPowOn = cycle - lcdOnCycle; // TODO can move???

        if (cycle == nextNonIdleCycle && isOn()) {
            int cycFromImg = (int) (cycFromPowOn % IMAGE_CYCLE_DURATION);
            int currentLine = (int) (cycFromImg / LINE_CYCLE_DURATION);
            int cycFromLn = cycFromImg % LINE_CYCLE_DURATION;
            reallyCycle(cycle, currentLine, cycFromLn);
        }
    }

    private void reallyCycle(long cycle, int currentLine, int cycFromLn) {
        // System.out.println("cycle " + cycle + " current line " + currentLine + " ly "
        // + lcdRegs.get(LCDReg.LY) + " cycles from line " + cycFromLn);
        modifyLYorLYC(LCDReg.LY, currentLine);
        if (currentLine <= 143) {
            switch (cycFromLn) {
            case MODE2_DURATION:
                // System.out.println("switch to mode 3");
                nextNonIdleCycle += MODE3_DURATION;
                setMode(3);
                break;
            case MODE2_DURATION + MODE3_DURATION:
                // System.out.println("switch to mode 0");
                nextNonIdleCycle += MODE0_DURATION;
                nextImageBuilder.setLine(currentLine, computeLine(currentLine));
                setMode(0);
                break;
            case 0:
                if (currentLine == 143) {
                    // System.out.println("switch to mode 1");
                    nextNonIdleCycle += LINE_CYCLE_DURATION;
                    setMode(1);
                } else {
                    // System.out.println("switch to mode 2");
                    nextNonIdleCycle += MODE2_DURATION;
                    setMode(2);
                }
                break;
            }
        } else {
            if (currentLine == 144) {
                // System.out.println("draw virtual line " + currentLine);
                displayedImage = nextImageBuilder.build();
                nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
                nextNonIdleCycle += LINE_CYCLE_DURATION;
                cpu.requestInterrupt(Interrupt.VBLANK);
            } else {
                // System.out.println("draw virtual line " + currentLine);
                nextNonIdleCycle += LINE_CYCLE_DURATION;
            }

            if (currentLine == 153) {
                // System.out.println("switch to mode 2 from vblank ");
                winY = 0;
                nextNonIdleCycle += MODE2_DURATION;
                setMode(2);
            }
        }
    }

    private void compareLYandLYC() {
        lcdRegs.setBit(LCDReg.STAT, STAT.LYC_EQ_LY, false);

        if (lcdRegs.get(LCDReg.LY) == lcdRegs.get(LCDReg.LYC)) {
            lcdRegs.setBit(LCDReg.STAT, STAT.LYC_EQ_LY, true);
            // TODO request STAT interrupt if appropriate, clean up code
        }
    }

    private void turnOff() {
        // TODO power off only possible during VBLANK
        setMode(0);
        modifyLYorLYC(LCDReg.LY, 0);
        nextNonIdleCycle = Long.MAX_VALUE;
    }

    private void turnOn(long cycle) {
        lcdOnCycle = cycle;
        nextNonIdleCycle = cycle + MODE2_DURATION;
        setMode(2);
    }

    private boolean isOn() {
        return lcdRegs.testBit(LCDReg.LCDC, LCDC.LCD_STATUS);
    }

    private void setMode(int mode) {
        Preconditions.checkArgument(mode >= 0 && mode < 4, "The mode must be between 0 and 3");
        lcdRegs.setBit(LCDReg.STAT, STAT.MODE0, Bits.test(mode, 0));
        lcdRegs.setBit(LCDReg.STAT, STAT.MODE1, Bits.test(mode, 1));

        switch (mode) {
        case 0:
            if (lcdRegs.testBit(LCDReg.STAT, STAT.INT_MODE0)) {
                cpu.requestInterrupt(Interrupt.LCD_STAT);
            }
            break;
        case 1:
            if (lcdRegs.testBit(LCDReg.STAT, STAT.INT_MODE1)) {
                cpu.requestInterrupt(Interrupt.LCD_STAT);
            }
            break;
        case 2:
            if (lcdRegs.testBit(LCDReg.STAT, STAT.INT_MODE2)) {
                cpu.requestInterrupt(Interrupt.LCD_STAT);
            }
            break;
        default:
            break;
        }
    }

    private LcdImageLine computeLine(int lineIndex) {
        LcdImageLine nextLine = BLANK_LCD_IMAGE_LINE, bgSpriteLine = BLANK_LCD_IMAGE_LINE,
                fgSpriteLine = BLANK_LCD_IMAGE_LINE;

        int adjustedWX = lcdRegs.get(LCDReg.WX) - WX_OFFSET;

        if (lcdRegs.testBit(LCDReg.LCDC, LCDC.OBJ)) {
            List<Integer> bgSpriteInfoL = new ArrayList<>(10);
            List<Integer> fgSpriteInfoL = new ArrayList<>(10);
            Integer[] spriteInfo = spritesIntersectingLine(lineIndex);

            for (int i = 0; i < spriteInfo.length; ++i) {
                boolean isInBG = Bits.test(readAttr(i, DISPLAY_DATA.ATTRIBUTES), ATTRIBUTES.BEHIND_BG);
                if (isInBG) {
                    bgSpriteInfoL.add(spriteInfo[i]);
                } else {
                    fgSpriteInfoL.add(spriteInfo[i]);
                }
            }

            Integer[] bgSpriteInfo = bgSpriteInfoL.toArray(new Integer[0]);
            Integer[] fgSpriteInfo = fgSpriteInfoL.toArray(new Integer[0]);

            bgSpriteLine = computeSpriteLine(bgSpriteInfo, lineIndex);
            fgSpriteLine = computeSpriteLine(fgSpriteInfo, lineIndex);
        }

        nextLine = bgSpriteLine;

        if (lcdRegs.testBit(LCDReg.LCDC, LCDC.BG)) {
            LcdImageLine nextBGLine = computeBGLine(lineIndex).extractWrapped(lcdRegs.get(LCDReg.SCX), LCD_WIDTH);
            nextLine = nextLine.below(nextBGLine, computeMeldOpacity(nextLine, nextBGLine));
        }

        if (lineIndex >= lcdRegs.get(LCDReg.WY) && isWindowActive()) {
            LcdImageLine winLine = computeWinLine(adjustedWX, LCD_WIDTH);
            nextLine = nextLine.join(winLine, adjustedWX);
            if (lineIndex >= 120) {
                // System.out.println(nextLine.getMsb().toString());
                // System.out.println(nextLine.getLsb().toString());
            }
        }

        nextLine = nextLine.below(fgSpriteLine);

        return nextLine;
    }

    private LcdImageLine computeBGLine(int lineIndex) {
        LcdImageLine.Builder nextBGLineBuilder = new LcdImageLine.Builder(BG_SIZE);

        for (int i = 0; i < BG_TILE_SIZE; ++i) {
            int bgAddress;

            int tileIndex = Math.floorDiv(Math.floorMod(lcdRegs.get(LCDReg.SCY) + lineIndex, BG_SIZE), TILE_SIZE)
                    * BG_TILE_SIZE + i;

            if (lcdRegs.testBit(LCDReg.LCDC, LCDC.BG_AREA)) {
                bgAddress = AddressMap.BG_DISPLAY_DATA[1] + tileIndex;
            } else {
                bgAddress = AddressMap.BG_DISPLAY_DATA[0] + tileIndex;
            }

            int tileTypeIndex = read(bgAddress);

            nextBGLineBuilder.setBytes(i * Byte.SIZE, Bits.reverse8(tileLineMSB(tileTypeIndex, lineIndex)),
                    Bits.reverse8(tileLineLSB(tileTypeIndex, lineIndex)));
        }

        return nextBGLineBuilder.build().mapColors(lcdRegs.get(LCDReg.BGP));
    }

    private LcdImageLine computeWinLine(int adjustedWX, int width) {
        Preconditions.checkArgument(width % TILE_SIZE == 0, "The width must be a multiple of the tile size");
        LcdImageLine.Builder nextWinLineBuilder = new LcdImageLine.Builder(width);

        for (int i = 0; i < width / TILE_SIZE; ++i) {
            int winAddress;

            int tileIndex = Math.floorDiv(winY, TILE_SIZE) * WIN_TILE_SIZE + i;

            if (lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN_AREA)) {
                winAddress = AddressMap.BG_DISPLAY_DATA[1] + tileIndex;
            } else {
                winAddress = AddressMap.BG_DISPLAY_DATA[0] + tileIndex;
            }

            // System.out.println("win address " + winAddress);

            int winTypeIndex = read(winAddress);

            nextWinLineBuilder.setBytes(i * Byte.SIZE, Bits.reverse8(tileLineMSB(winTypeIndex, winY)),
                    Bits.reverse8(tileLineLSB(winTypeIndex, winY)));
        }

        winY++;

        return nextWinLineBuilder.build().shift(-adjustedWX).mapColors(lcdRegs.get(LCDReg.BGP));
    }

    private LcdImageLine computeSpriteLine(Integer[] spriteInfo, int lineIndex) {
        LcdImageLine[] spriteLines = new LcdImageLine[spriteInfo.length];
        LcdImageLine spriteLine = BLANK_LCD_IMAGE_LINE;

        for (int i = 0; i < spriteInfo.length; ++i) {
            LcdImageLine.Builder spriteLineBuilder = new LcdImageLine.Builder(LCD_WIDTH);
            LcdImageLine indSpriteLine;
            int spriteIndex = unpackIndex(spriteInfo[i]);
            int spriteX = unpackX(spriteInfo[i]) - SPRITE_XOFFSET;
            int spriteAttrMisc = readAttr(spriteIndex, DISPLAY_DATA.ATTRIBUTES);
            boolean spritePalette = Bits.test(spriteAttrMisc, ATTRIBUTES.PALETTE.index());
            boolean hFlip = Bits.test(spriteAttrMisc, ATTRIBUTES.FLIP_H.index());
            boolean vFlip = Bits.test(spriteAttrMisc, ATTRIBUTES.FLIP_V.index());
            int spriteTileIndex = readAttr(spriteIndex, DISPLAY_DATA.TILE_INDEX);

            if (hFlip) {
                spriteLineBuilder.setBytes(0, tileLineMSB(spriteTileIndex, spriteIndex, lineIndex, vFlip),
                        tileLineLSB(spriteTileIndex, spriteIndex, lineIndex, vFlip));
            } else {
                spriteLineBuilder.setBytes(0,
                        Bits.reverse8(tileLineMSB(spriteTileIndex, spriteIndex, lineIndex, vFlip)),
                        Bits.reverse8(tileLineLSB(spriteTileIndex, spriteIndex, lineIndex, vFlip)));
            }

            indSpriteLine = spriteLineBuilder.build().shift(-spriteX);

            if (spritePalette) {
                indSpriteLine = indSpriteLine.mapColors(lcdRegs.get(LCDReg.OBP1));
            } else {
                indSpriteLine = indSpriteLine.mapColors(lcdRegs.get(LCDReg.OBP0));
            }

            spriteLines[i] = indSpriteLine;
        }

        for (int i = spriteLines.length - 1; i >= 0; --i) {
            spriteLine = spriteLine.below(spriteLines[i]);
        }

        return spriteLine;
    }

    private int tileLineMSB(int tileTypeIndex, int lineIndex) {
        return read(tileTypeAddress(tileTypeIndex, lineIndex) + 1);
    }

    private int tileLineMSB(int tileTypeIndex, int tileIndex, int lineIndex, boolean vFlipped) {
        return read(tileTypeAddressS(tileTypeIndex, tileIndex, lineIndex, vFlipped) + 1); // TODO FIGURE OUT -SPRITEY
    }

    private int tileLineLSB(int tileTypeIndex, int lineIndex) {
        return read(tileTypeAddress(tileTypeIndex, lineIndex));
    }

    private int tileLineLSB(int tileTypeIndex, int tileIndex, int lineIndex, boolean vFlipped) {
        return read(tileTypeAddressS(tileTypeIndex, tileIndex, lineIndex, vFlipped)); // TODO FIGURE OUT -SPRITEY
    }

    private int tileTypeAddress(int tileTypeIndex, int lineIndex) {
        if (lcdRegs.testBit(LCDReg.LCDC, LCDC.TILE_SOURCE)) {
            return AddressMap.TILE_SOURCE[1] + tileTypeIndex * BYTES_PER_TILE + Math.floorMod(lineIndex, TILE_SIZE) * 2;
        } else {
            if (tileTypeIndex >= 0 && tileTypeIndex < 128) {
                return AddressMap.TILE_SOURCE[0] + (tileTypeIndex + 128) * BYTES_PER_TILE
                        + Math.floorMod(lineIndex, TILE_SIZE) * 2;
            } else if (tileTypeIndex >= 128 && tileTypeIndex < 256) {
                return AddressMap.TILE_SOURCE[0] + (tileTypeIndex - 128) * BYTES_PER_TILE
                        + Math.floorMod(lineIndex, TILE_SIZE) * 2;
            } else {
                throw new IllegalArgumentException("tile_type_index wrong! " + tileTypeIndex);
            }
        }
    }

    private int tileTypeAddressS(int tileTypeIndex, int tileIndex, int lineIndex, boolean vFlipped) {
        // TODO When double character composition, only even-numbered indexes can be
        // selected, when odd will be the same as even, how to do this?
        int height = getHeight();
        int spriteY = read(AddressMap.OAM_START + tileIndex * 4);

        if (vFlipped) {
            return AddressMap.TILE_SOURCE[1] + tileTypeIndex * BYTES_PER_TILE
                    + (height - Math.floorMod(lineIndex - spriteY, height)) * 2;
        } else {
            return AddressMap.TILE_SOURCE[1] + tileTypeIndex * BYTES_PER_TILE
                    + Math.floorMod(lineIndex - spriteY, height) * 2;
        }
    }

    private Integer[] spritesIntersectingLine(int lineIndex) {
        int scanIndex = 0, foundSprites = 0;
        int spriteHeight = getHeight();

        Integer[] intersect = new Integer[MAX_SPRITES]; // TODO replace with list?

        while (foundSprites < MAX_SPRITES && scanIndex < OAM_SPRITES) {
            int spriteY = readAttr(scanIndex, DISPLAY_DATA.Y_COORD) - SPRITE_YOFFSET;
            if (lineIndex >= spriteY && lineIndex < spriteY + spriteHeight) {
                intersect[foundSprites] = packSpriteInfo(scanIndex);
                foundSprites++;
            }

            scanIndex++;
        }

        Integer[] intersectIndex = trimIntArray(intersect, foundSprites);

        Arrays.sort(intersectIndex); // TODO replace with System.sortarray call?

        return intersectIndex;
    }

    private Integer[] trimIntArray(Integer[] array, int trimIndex) {
        Integer[] intersectIndex = new Integer[trimIndex];

        for (int i = 0; i < trimIndex; ++i) {
            intersectIndex[i] = array[i];
        }

        return intersectIndex;
    }

    private int getHeight() {
        // Sprite size: 8 x 8 or 8 x 16
        return TILE_SIZE * (lcdRegs.testBit(LCDReg.LCDC, LCDC.OBJ_SIZE) ? 2 : 1);
    }

    private int packSpriteInfo(int spriteIndex) {
        return readAttr(spriteIndex, DISPLAY_DATA.X_COORD) << Byte.SIZE | spriteIndex;
    }

    private int unpackX(int spriteInfo) {
        return Bits.extract(spriteInfo, Byte.SIZE, Byte.SIZE);
    }

    private int unpackIndex(int spriteInfo) {
        return Bits.clip(Byte.SIZE, spriteInfo);
    }

    private int readAttr(int spriteIndex, DISPLAY_DATA attr) {
        return read(AddressMap.OAM_START + Objects.checkIndex(spriteIndex, OAM_SPRITES) * SPRITE_ATTR_BYTES
                + attr.ordinal());
    }

    private BitVector computeMeldOpacity(LcdImageLine below, LcdImageLine over) {
        return below.getOpacity().and(over.getOpacity().not()).not();
    }

    private boolean isWindowActive() {
        int adjustedWX = lcdRegs.get(LCDReg.WX) - WX_OFFSET;

        // System.out.println("adjusted WX: " + adjustedWX + " lcdc win: " +
        // lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN));

        if (adjustedWX >= 0 && adjustedWX < 160 && lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN)) {
            return true;
        }

        return false;
    }

    @Override
    public void attachTo(Bus bus) {
        bus.attach(this);
        dmaController.setBus(bus);
    }

    @Override
    public int read(int address) {
        // TODO if drawing, cpu cannot access VRAM and OAM

        if (Preconditions.checkBits16(address) >= AddressMap.VRAM_START && address < AddressMap.VRAM_END) {
            return videoRamController.read(address);
        } else if (address >= AddressMap.OAM_START && address < AddressMap.OAM_END) {
            return oamRamController.read(address);
        } else if (address >= AddressMap.REGS_LCD_START && address < AddressMap.REGS_LCD_END) {
            return lcdRegs.get(address - AddressMap.REGS_LCD_START);
        }

        return NO_DATA;
    }

    @Override
    public void write(int address, int data) {
        Preconditions.checkBits8(data);

        // TODO if drawing, cpu cannot access VRAM and OAM

        if (Preconditions.checkBits16(address) >= AddressMap.VRAM_START && address < AddressMap.VRAM_END) {
            // TODO cannot access during mode 3
            videoRamController.write(address, data);
        } else if (address >= AddressMap.OAM_START && address < AddressMap.OAM_END) {
            // TODO cannot access during mode 2 or 3
            oamRamController.write(address, data);
        } else if (address >= AddressMap.REGS_LCD_START && address < AddressMap.REGS_LCD_END) {
            switch (address) {
            case AddressMap.REG_LCDC:
                lcdRegs.set(LCDReg.LCDC, data);
                if (!lcdRegs.testBit(LCDReg.LCDC, LCDC.LCD_STATUS)) {
                    turnOff();
                }
                break;
            case AddressMap.REG_LCD_STAT:
                lcdRegs.set(LCDReg.STAT, (data & 0b1111_1000) | (lcdRegs.get(LCDReg.STAT) & 0b0000_0111));
                break;
            case AddressMap.REG_SCY:
                lcdRegs.set(LCDReg.SCY, data);
                break;
            case AddressMap.REG_SCX:
                lcdRegs.set(LCDReg.SCX, data);
                break;
            case AddressMap.REG_LY:
                modifyLYorLYC(LCDReg.LY, 0);
                break;
            case AddressMap.REG_LYC:
                modifyLYorLYC(LCDReg.LYC, data);
                break;
            case AddressMap.REG_DMA:
                lcdRegs.set(LCDReg.DMA, data);
                dmaController.start(data);
                break;
            case AddressMap.REG_BGP:
                lcdRegs.set(LCDReg.BGP, data);
                break;
            case AddressMap.REG_OBP0:
                lcdRegs.set(LCDReg.OBP0, data);
                break;
            case AddressMap.REG_OBP1:
                lcdRegs.set(LCDReg.OBP1, data);
                break;
            case AddressMap.REG_WY:
                lcdRegs.set(LCDReg.WY, data);
                break;
            case AddressMap.REG_WX:
                lcdRegs.set(LCDReg.WX, data);
                break;
            }
        }
    }

    private void modifyLYorLYC(LCDReg reg, int data) {
        Preconditions.checkArgument(reg == LCDReg.LY || reg == LCDReg.LYC);

        lcdRegs.set(reg, Preconditions.checkBits8(data));

        if (lcdRegs.get(LCDReg.LY) == lcdRegs.get(LCDReg.LYC)) {
            lcdRegs.setBit(LCDReg.STAT, STAT.LYC_EQ_LY, true);
            cpu.requestInterrupt(Interrupt.LCD_STAT);
        } else {
            lcdRegs.setBit(LCDReg.STAT, STAT.LYC_EQ_LY, false);
        }
    }
}
