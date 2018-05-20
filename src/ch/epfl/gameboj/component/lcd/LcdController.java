package ch.epfl.gameboj.component.lcd;

import static ch.epfl.gameboj.component.lcd.LcdImage.BLANK_LCD_IMAGE;
import static ch.epfl.gameboj.component.lcd.LcdImageLine.BLANK_LCD_IMAGE_LINE;
import static ch.epfl.gameboj.component.memory.OamRamController.SPRITE_XOFFSET;
import static ch.epfl.gameboj.component.memory.OamRamController.SPRITE_YOFFSET;

import java.util.ArrayList;
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
import ch.epfl.gameboj.component.memory.OamRamController;
import ch.epfl.gameboj.component.memory.OamRamController.ATTRIBUTES;
import ch.epfl.gameboj.component.memory.OamRamController.DISPLAY_DATA;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;
import ch.epfl.gameboj.component.memory.VideoRamController;

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

    private static final int TILE_SIZE = 8;
    // Background size: 256 x 256, 32 x 32 tiles
    private static final int BG_SIZE = 256;
    private static final int BG_TILE_SIZE = BG_SIZE / TILE_SIZE;
    // Resolution: 160 x 144, 20 x 18 tiles
    public static final int LCD_WIDTH = 160, LCD_HEIGHT = 144;
    private static final int WIN_SIZE = BG_SIZE, WIN_TILE_SIZE = BG_TILE_SIZE;
    // Max sprites: 40 per screen, 10 per line
    private static final int WX_OFFSET = 7;
    // Cycle durations
    private static final int MODE2_DURATION = 20, MODE3_DURATION = 43, MODE0_DURATION = 51;
    public static final int LINE_CYCLE_DURATION = MODE2_DURATION + MODE3_DURATION + MODE0_DURATION;
    public static final int IMAGE_CYCLE_DURATION = 154 * LINE_CYCLE_DURATION;
    private LcdImage displayedImage = BLANK_LCD_IMAGE;
    private final VideoRamController videoRamController;
    private final OamRamController oamRamController;
    private final Cpu cpu;
    private long nextNonIdleCycle = Long.MAX_VALUE;
    private int winY;
    private LcdImage.Builder nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
    private final DmaController dmaController = DmaController.getDmaController();
    private final RegisterFile<Register> lcdRegs = new RegisterFile<>(LCDReg.values());
    private long lcdOnCycle;

    int prevMode;
    private int currentImage;
    long cycFromImg;
    long cycFromLn;
    private long cyc;

    // TODO can only access HRAM during quick copy

    /**
     * Construit un contrôleur LCD.
     * 
     * @param cpu
     *            le CPU avec lequel le contrôleur interagit
     */
    public LcdController(Cpu cpu) {
        this.cpu = Objects.requireNonNull(cpu);

        videoRamController = new VideoRamController(new RamController(new Ram(AddressMap.VRAM_SIZE), AddressMap.VRAM_START));
        oamRamController = new OamRamController(new RamController(new Ram(AddressMap.OAM_SIZE), AddressMap.OAM_START));
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
            windowBuilder.setLine(y, computeWinLine(0, y, WIN_SIZE));
        }
        return windowBuilder.build();
    }

    public LcdImage getSprites() { // TODO whole screen or just lcd?
        LcdImage.Builder spriteBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
        
        int height = getHeight();

        for (int y = 0; y < LCD_HEIGHT; ++y) {
            spriteBuilder.setLine(y, computeSpriteLine(oamRamController.spritesIntersectingLine(y, height), y));
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

        cyc = cycle;

        if (isOn()) {
            long cycFromPowOn = cycle - lcdOnCycle; 
            cycFromImg = (int) (cycFromPowOn % IMAGE_CYCLE_DURATION);
            currentImage = (int) Math.floorDiv(cycFromPowOn, IMAGE_CYCLE_DURATION);

            if (cycle == nextNonIdleCycle) {
                int currentLine = (int) (cycFromImg / LINE_CYCLE_DURATION);
                cycFromLn = cycFromImg % LINE_CYCLE_DURATION;
                reallyCycle(cycle, currentLine, (int) cycFromLn);
            }
        }
    }

    private void reallyCycle(long cycle, int currentLine, int cycFromLn) {       
        modifyLYorLYC(LCDReg.LY, currentLine);
        
        if (currentLine < 144) {
            switch (cycFromLn) {
            case MODE2_DURATION:
                nextNonIdleCycle += MODE3_DURATION;
                setMode(3);
                break;
            case MODE2_DURATION + MODE3_DURATION:
                nextNonIdleCycle += MODE0_DURATION;
                setMode(0);
                nextImageBuilder.setLine(currentLine, computeLine(currentLine));
                break;
            case 0:
                nextNonIdleCycle += MODE2_DURATION;
                setMode(2);
                break;
            }
        } else {
            nextNonIdleCycle += LINE_CYCLE_DURATION;
            if (currentLine == 144) {
                setMode(1);
                displayedImage = nextImageBuilder.build();
                nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
            } else if (currentLine == 153) {
                winY = 0;
            }
        }
    }

    private void turnOff() {
        // TODO power off only possible during VBLANK
        setMode(0);
        modifyLYorLYC(LCDReg.LY, 0);
        nextNonIdleCycle = Long.MAX_VALUE;
        System.out.println("turnoff");
    }

    private void turnOn(long cycle) {
        lcdOnCycle = cycle;
        nextNonIdleCycle = cycle;
        setMode(2);
    }

    private boolean isOn() {
        return lcdRegs.testBit(LCDReg.LCDC, LCDC.LCD_STATUS);
    }

    private void setMode(int mode) {
        Preconditions.checkArgument(mode >= 0 && mode < 4, "The mode must be between 0 and 3");
        lcdRegs.setBit(LCDReg.STAT, STAT.MODE0, Bits.test(mode, 0));
        lcdRegs.setBit(LCDReg.STAT, STAT.MODE1, Bits.test(mode, 1));

        if (currentImage == 1) {
            System.out.println("cycles: " + cyc + " since frame: " + cycFromImg + " | mode: " + prevMode + " -> " + mode);

        }
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
            if (currentImage == 1) {
                System.out.println("cycles: " + cyc + " since frame: " + cycFromImg + " | request VBLANK interrupt");
            }
            cpu.requestInterrupt(Interrupt.VBLANK); // TODO before or after lcd stat?
            break;
        case 2:
            if (lcdRegs.testBit(LCDReg.STAT, STAT.INT_MODE2)) {
                cpu.requestInterrupt(Interrupt.LCD_STAT);
            }
            break;
        default:
            break;
        }

        prevMode = mode;
    }

    private LcdImageLine computeLine(int lineIndex) {
        LcdImageLine nextLine = BLANK_LCD_IMAGE_LINE, bgSpriteLine = BLANK_LCD_IMAGE_LINE,
                fgSpriteLine = BLANK_LCD_IMAGE_LINE;

        int adjustedWX = lcdRegs.get(LCDReg.WX) - WX_OFFSET;
        
        int height = getHeight();

        if (lcdRegs.testBit(LCDReg.LCDC, LCDC.OBJ)) {
            List<Integer> bgSpriteInfoL = new ArrayList<>(10);
            List<Integer> fgSpriteInfoL = new ArrayList<>(10);
            Integer[] spriteInfo = oamRamController.spritesIntersectingLine(lineIndex, height);

            for (int i = 0; i < spriteInfo.length; ++i) {
                boolean isInBG = oamRamController.readAttr(i, ATTRIBUTES.BEHIND_BG);
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

        if (isBackgroundActive()) {
            LcdImageLine nextBGLine = computeBGLine(lineIndex).extractWrapped(lcdRegs.get(LCDReg.SCX), LCD_WIDTH);
            nextLine = nextLine.below(nextBGLine, computeMeldOpacity(nextLine, nextBGLine));
        }

        if (lineIndex >= lcdRegs.get(LCDReg.WY) && isWindowActive(adjustedWX)) {
            LcdImageLine winLine = computeWinLine(adjustedWX, winY++, LCD_WIDTH);
            nextLine = nextLine.join(winLine, adjustedWX);
        }

        nextLine = nextLine.below(fgSpriteLine);

        return nextLine;
    }

    private LcdImageLine computeBGLine(int lineIndex) {
        LcdImageLine.Builder nextBGLineBuilder = new LcdImageLine.Builder(BG_SIZE);

        for (int i = 0; i < BG_TILE_SIZE; ++i) {
            int bgAddress;

            int tileIndex = (Math.floorMod(lcdRegs.get(LCDReg.SCY) + lineIndex, BG_SIZE) / TILE_SIZE) * BG_TILE_SIZE + i;

            if (lcdRegs.testBit(LCDReg.LCDC, LCDC.BG_AREA)) {
                bgAddress = AddressMap.BG_DISPLAY_DATA[1] + tileIndex;
            } else {
                bgAddress = AddressMap.BG_DISPLAY_DATA[0] + tileIndex;
            }

            int tileTypeIndex = videoRamController.read(bgAddress);

            nextBGLineBuilder.setBytes(i * Byte.SIZE, 
                    Bits.reverse8(videoRamController.tileLineBytes(tileTypeIndex, bgTileLineIndex(lineIndex), lcdRegs.testBit(LCDReg.LCDC, LCDC.TILE_SOURCE), true)),
                    Bits.reverse8(videoRamController.tileLineBytes(tileTypeIndex, bgTileLineIndex(lineIndex), lcdRegs.testBit(LCDReg.LCDC, LCDC.TILE_SOURCE), false)));
        }

        return nextBGLineBuilder.build().mapColors(lcdRegs.get(LCDReg.BGP));
    }

    private LcdImageLine computeWinLine(int adjustedWX, int lineIndex, int width) {
        Preconditions.checkArgument(width % TILE_SIZE == 0,
                "The width must be a multiple of the tile size. Was provided: " + width);
        Objects.checkIndex(lineIndex, WIN_SIZE);
        LcdImageLine.Builder nextWinLineBuilder = new LcdImageLine.Builder(width);

        for (int i = 0; i < width / TILE_SIZE; ++i) {
            int winAddress;

            int tileIndex = (lineIndex / TILE_SIZE) * WIN_TILE_SIZE + i;

            if (lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN_AREA)) {
                winAddress = AddressMap.BG_DISPLAY_DATA[1] + tileIndex;
            } else {
                winAddress = AddressMap.BG_DISPLAY_DATA[0] + tileIndex;
            }

            int winTypeIndex = videoRamController.read(winAddress);

            nextWinLineBuilder.setBytes(i * Byte.SIZE, 
                    Bits.reverse8(videoRamController.tileLineBytes(winTypeIndex, winTileLineIndex(lineIndex), lcdRegs.testBit(LCDReg.LCDC, LCDC.TILE_SOURCE), true)),
                    Bits.reverse8(videoRamController.tileLineBytes(winTypeIndex, winTileLineIndex(lineIndex), lcdRegs.testBit(LCDReg.LCDC, LCDC.TILE_SOURCE), false)));
        }

        return nextWinLineBuilder.build().shift(-adjustedWX).mapColors(lcdRegs.get(LCDReg.BGP));
    }

    private LcdImageLine computeSpriteLine(Integer[] spriteInfo, int lineIndex) {
        Objects.checkIndex(lineIndex, LCD_HEIGHT);
        LcdImageLine[] spriteLines = new LcdImageLine[spriteInfo.length];
        LcdImageLine spriteLine = BLANK_LCD_IMAGE_LINE;
        
        int height = getHeight();

        for (int i = 0; i < spriteInfo.length; ++i) {
            LcdImageLine.Builder spriteLineBuilder = new LcdImageLine.Builder(LCD_WIDTH);
            LcdImageLine indSpriteLine;
            int spriteIndex = unpackIndex(spriteInfo[i]);
            int spriteX = unpackX(spriteInfo[i]) - SPRITE_XOFFSET;
            boolean spritePalette = oamRamController.readAttr(spriteIndex, ATTRIBUTES.PALETTE);
            boolean hFlip = oamRamController.readAttr(spriteIndex, ATTRIBUTES.FLIP_H);
            boolean vFlip = oamRamController.readAttr(spriteIndex, ATTRIBUTES.FLIP_V);
            int spriteTileIndex = oamRamController.readAttr(spriteIndex, DISPLAY_DATA.TILE_INDEX);
            int spriteY = oamRamController.readAttr(spriteIndex, DISPLAY_DATA.Y_COORD) - SPRITE_YOFFSET;
            
            Preconditions.checkArgument(spriteY >= -SPRITE_YOFFSET && spriteY < LCD_HEIGHT, "Got a sprite y value of " + spriteY);

            if (hFlip) {
                spriteLineBuilder.setBytes(0,
                        videoRamController.tileLineBytes(spriteTileIndex, spriteTileLineIndex(lineIndex, spriteY, vFlip, height), true),
                        videoRamController.tileLineBytes(spriteTileIndex, spriteTileLineIndex(lineIndex, spriteY, vFlip, height), false));
            } else {
                spriteLineBuilder.setBytes(0,
                        Bits.reverse8(videoRamController.tileLineBytes(spriteTileIndex, spriteTileLineIndex(lineIndex, spriteY, vFlip, height), true)),
                        Bits.reverse8(videoRamController.tileLineBytes(spriteTileIndex, spriteTileLineIndex(lineIndex, spriteY, vFlip, height), false)));
            }

            indSpriteLine = spriteLineBuilder.build().shift(-spriteX).mapColors(lcdRegs.get(spritePalette ? LCDReg.OBP1 : LCDReg.OBP0));

            spriteLines[i] = indSpriteLine;
        }

        for (int i = spriteLines.length - 1; i >= 0; --i) {
            spriteLine = spriteLine.below(spriteLines[i]);
        }

        return spriteLine;
    }
    
    private int bgTileLineIndex(int lineIndex) {
        return (lcdRegs.get(LCDReg.SCY) + Objects.checkIndex(lineIndex, BG_SIZE)) % TILE_SIZE;
    }
    
    private int winTileLineIndex(int lineIndex) {
        return Objects.checkIndex(lineIndex, WIN_SIZE) % TILE_SIZE;
    }
    
    private int spriteTileLineIndex(int lineIndex, int spriteY, boolean vFlip, int height) {
        Objects.checkIndex(lineIndex, LCD_HEIGHT);
        if (vFlip) {
            return (height - Math.floorMod(lineIndex - spriteY, height));
        } else {
            return Math.floorMod(lineIndex - spriteY, height);
        }
    }

    private int getHeight() {
        // Sprite size: 8 x 8 or 8 x 16
        return TILE_SIZE * (lcdRegs.testBit(LCDReg.LCDC, LCDC.OBJ_SIZE) ? 2 : 1);
    }

    private int unpackX(int spriteInfo) {
        return Bits.extract(spriteInfo, Byte.SIZE, Byte.SIZE);
    }

    private int unpackIndex(int spriteInfo) {
        return Bits.clip(Byte.SIZE, spriteInfo);
    }

    private BitVector computeMeldOpacity(LcdImageLine below, LcdImageLine over) {
        return below.getOpacity().and(over.getOpacity().not()).not();
    }

    private boolean isWindowActive(int adjustedWX) {
//         System.out.println("adjusted WX: " + adjustedWX + " lcdc win: " +
//         lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN));

        return (adjustedWX >= 0 && adjustedWX < 160 && lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN));
    }
    
    private boolean isBackgroundActive() {
        return lcdRegs.testBit(LCDReg.LCDC, LCDC.BG);
    }

    @Override
    public void attachTo(Bus bus) {
        Objects.requireNonNull(bus, "You must provide LcdController with a non-null bus");
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
//            return lcdRegs.get(address - AddressMap.REGS_LCD_START);
            switch (address) {
            case AddressMap.REG_LCDC:
                return lcdRegs.get(LCDReg.LCDC);
            case AddressMap.REG_LCD_STAT:
                return lcdRegs.get(LCDReg.STAT);
            case AddressMap.REG_SCY:
                return lcdRegs.get(LCDReg.SCY);
            case AddressMap.REG_SCX:
                return lcdRegs.get(LCDReg.SCX);
            case AddressMap.REG_LY:
                return lcdRegs.get(LCDReg.LY);
            case AddressMap.REG_LYC:
                return lcdRegs.get(LCDReg.LYC);
            case AddressMap.REG_DMA:
                return lcdRegs.get(LCDReg.DMA);
            case AddressMap.REG_BGP:
                return lcdRegs.get(LCDReg.BGP);
            case AddressMap.REG_OBP0:
                return lcdRegs.get(LCDReg.OBP0);
            case AddressMap.REG_OBP1:
                return lcdRegs.get(LCDReg.OBP1);
            case AddressMap.REG_WY:
                return lcdRegs.get(LCDReg.WY);
            case AddressMap.REG_WX:
                return lcdRegs.get(LCDReg.WX);
            }
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
        if (currentImage == 1 && reg == LCDReg.LY && data != lcdRegs.get(LCDReg.LY))
            System.out.println("cycles: " + cyc + " since frame: " + cycFromImg + " | LY: " + lcdRegs.get(LCDReg.LY)
                    + " -> " + data);

        lcdRegs.set(reg, Preconditions.checkBits8(data));

        if (lcdRegs.get(LCDReg.LY) == lcdRegs.get(LCDReg.LYC)) {
            lcdRegs.setBit(LCDReg.STAT, STAT.LYC_EQ_LY, true);
            if (lcdRegs.testBit(LCDReg.STAT, STAT.INT_LYC)) {
                cpu.requestInterrupt(Interrupt.LCD_STAT);
            }
        } else {
            lcdRegs.setBit(LCDReg.STAT, STAT.LYC_EQ_LY, false);
        }
    }
}
