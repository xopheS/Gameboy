package ch.epfl.gameboj.component.lcd;

import static ch.epfl.gameboj.component.lcd.LcdImage.BLANK_LCD_IMAGE;
import static ch.epfl.gameboj.component.lcd.LcdImageLine.BLANK_LCD_IMAGE_LINE;
import static ch.epfl.gameboj.component.memory.OamRamController.SPRITES_PER_LINE;
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
import ch.epfl.gameboj.component.memory.VideoRamController;

public final class LcdController implements Component, Clocked {

    private enum LCDReg implements Register { LCDC, STAT, SCY, SCX, LY, LYC, DMA, BGP, OBP0, OBP1, WY, WX }

    private enum LCDC implements Bit { BG, OBJ, OBJ_SIZE, BG_AREA, TILE_SOURCE, WIN, WIN_AREA, LCD_STATUS }

    private enum STAT implements Bit { MODE0, MODE1, LYC_EQ_LY, INT_MODE0, INT_MODE1, INT_MODE2, INT_LYC, UNUSED7 }

    // A tile is 8 x 8 bits
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

    /**
     * Construit un contrôleur LCD.
     * 
     * @param cpu
     *            le CPU avec lequel le contrôleur interagit
     */
    public LcdController(Cpu cpu) {
        this.cpu = Objects.requireNonNull(cpu);

        videoRamController = new VideoRamController(new Ram(AddressMap.VRAM_SIZE), AddressMap.VRAM_START);
        oamRamController = new OamRamController(new Ram(AddressMap.OAM_SIZE), AddressMap.OAM_START);
    }

    public LcdImage currentImage() {
        return Objects.requireNonNull(displayedImage, "Displayed image cannot be null");
    }
    
    public LcdImage getBackground() {
        LcdImage.Builder backgroundBuilder = new LcdImage.Builder(BG_SIZE, BG_SIZE);
        for (int y = 0; y < BG_SIZE; ++y) {
            backgroundBuilder.setLine(y, computeBGorWinLine(y, true));
        }
        return backgroundBuilder.build();
    }

    public LcdImage getWindow() {
        LcdImage.Builder windowBuilder = new LcdImage.Builder(WIN_SIZE, WIN_SIZE);
        for (int y = 0; y < WIN_SIZE; ++y) {
            windowBuilder.setLine(y, computeBGorWinLine(y, false));
        }
        return windowBuilder.build();
    }

    public LcdImage[] getSprites() {
        LcdImage.Builder bgSpriteBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
        LcdImage.Builder fgSpriteBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
        
        int spriteHeight = getHeight();

        for (int y = 0; y < LCD_HEIGHT; ++y) {
            Integer[] spriteInfo = oamRamController.spritesIntersectingLine(y, spriteHeight);
            Integer[][] spriteLayerInfo = spriteLayerInfo(spriteInfo);
            bgSpriteBuilder.setLine(y, computeSpriteLine(spriteLayerInfo[0], y, spriteHeight));
            fgSpriteBuilder.setLine(y, computeSpriteLine(spriteLayerInfo[1], y, spriteHeight));
        }
        return new LcdImage[] { bgSpriteBuilder.build(), fgSpriteBuilder.build() };
    }

    @Override
    public void cycle(long cycle) {
        if (nextNonIdleCycle == Long.MAX_VALUE && isOn()) {
            turnOn(cycle);
        }

        if (dmaController.isActive()) {
            dmaController.copy();
        }

        if (isOn() && cycle == nextNonIdleCycle) {
            long cycFromPowOn = cycle - lcdOnCycle; 
            long cycFromImg = (int) (cycFromPowOn % IMAGE_CYCLE_DURATION);
            int currentLine = (int) (cycFromImg / LINE_CYCLE_DURATION);
            long cycFromLn = cycFromImg % LINE_CYCLE_DURATION;
            reallyCycle(currentLine, (int) cycFromLn);
        }
    }

    private void reallyCycle(int currentLine, int cycFromLn) {              
        if (currentLine < LCD_HEIGHT) {
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
                modifyLYorLYC(LCDReg.LY, currentLine);
                setMode(2);
                break;
            }
        } else {
            if (currentLine == LCD_HEIGHT) {
                setMode(1);
                displayedImage = nextImageBuilder.build();
                nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
                winY = 0;
            }
            
            nextNonIdleCycle += LINE_CYCLE_DURATION;
            modifyLYorLYC(LCDReg.LY, currentLine);
        }
    }

    private void turnOff() {
        setMode(0);
        modifyLYorLYC(LCDReg.LY, 0);
        nextNonIdleCycle = Long.MAX_VALUE;
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
            cpu.requestInterrupt(Interrupt.VBLANK);
            break;
        case 2:
            if (lcdRegs.testBit(LCDReg.STAT, STAT.INT_MODE2)) {
                cpu.requestInterrupt(Interrupt.LCD_STAT);
            }
            break;
        }
    }

    private LcdImageLine computeLine(int lineIndex) {
        LcdImageLine nextLine = BLANK_LCD_IMAGE_LINE, fgSpriteLine = BLANK_LCD_IMAGE_LINE;

        int adjustedWX = lcdRegs.get(LCDReg.WX) - WX_OFFSET;
        
        int spriteHeight = getHeight();

        if (areSpritesActive()) {
            Integer[] spriteInfo = oamRamController.spritesIntersectingLine(lineIndex, spriteHeight);
            Integer[][] spriteLayerInfo = spriteLayerInfo(spriteInfo);

            nextLine = computeSpriteLine(spriteLayerInfo[0], lineIndex, spriteHeight);
            
            fgSpriteLine = computeSpriteLine(spriteLayerInfo[1], lineIndex, spriteHeight);          
        }

        if (isBackgroundActive()) {
            LcdImageLine nextBGLine = computeBGorWinLine(lineIndex, true)
                .extractWrapped(lcdRegs.get(LCDReg.SCX), LCD_WIDTH);
            nextLine = nextLine.below(nextBGLine, computeMeldOpacity(nextLine, nextBGLine));
        }

        if (lineIndex >= lcdRegs.get(LCDReg.WY) && isWindowActive(adjustedWX)) {
            nextLine = nextLine.join(computeBGorWinLine(winY, false)
                .extractZeroExtended(-adjustedWX, LCD_WIDTH), adjustedWX);
            winY++;
        }

        nextLine = nextLine.below(fgSpriteLine);

        return nextLine;
    }

    private LcdImageLine computeBGorWinLine(int lineIndex, boolean bg) {
        LcdImageLine.Builder nextBGLineBuilder = new LcdImageLine.Builder(BG_SIZE);
        boolean tileSource = lcdRegs.testBit(LCDReg.LCDC, LCDC.TILE_SOURCE);

        for (int i = 0; i < BG_TILE_SIZE; ++i) {
            int addressInVram;
            int tileIndexInVram;

            if (bg) {
                tileIndexInVram = (((lcdRegs.get(LCDReg.SCY) + lineIndex) % BG_SIZE) / TILE_SIZE) * BG_TILE_SIZE + i;
            } else {
                tileIndexInVram = (lineIndex / TILE_SIZE) * WIN_TILE_SIZE + i;
            }

            if (bg ? lcdRegs.testBit(LCDReg.LCDC, LCDC.BG_AREA) : lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN_AREA)) {
                addressInVram = AddressMap.BG_DISPLAY_DATA[1] + tileIndexInVram;
            } else {
                addressInVram = AddressMap.BG_DISPLAY_DATA[0] + tileIndexInVram;
            }

            int tileTypeIndex = videoRamController.read(addressInVram);
            
            int tileLineIndex = bg ? bgTileLineIndex(lineIndex) : winTileLineIndex(lineIndex);
            
            int[] tileLineBytes = videoRamController.tileLineBytes(tileTypeIndex, tileLineIndex, tileSource);

            nextBGLineBuilder.setBytes(Byte.SIZE * i, Bits.reverse8(tileLineBytes[1]), Bits.reverse8(tileLineBytes[0]));
        }

        return nextBGLineBuilder.build().mapColors(lcdRegs.get(LCDReg.BGP));
    }

    private LcdImageLine computeSpriteLine(Integer[] spriteInfo, int lineIndex, int spriteHeight) {
        LcdImageLine[] spriteLines = new LcdImageLine[spriteInfo.length];
        LcdImageLine spriteLine = BLANK_LCD_IMAGE_LINE;

        for (int i = 0; i < spriteInfo.length; ++i) {
            LcdImageLine.Builder spriteLineBuilder = new LcdImageLine.Builder(LCD_WIDTH);
            int spriteIndex = unpackIndex(spriteInfo[i]);
            int spriteX = unpackX(spriteInfo[i]) - SPRITE_XOFFSET;
            boolean spritePalette = oamRamController.readAttr(spriteIndex, ATTRIBUTES.PALETTE);
            boolean hFlip = oamRamController.readAttr(spriteIndex, ATTRIBUTES.FLIP_H);
            boolean vFlip = oamRamController.readAttr(spriteIndex, ATTRIBUTES.FLIP_V);
            int spriteTileIndex = oamRamController.readAttr(spriteIndex, DISPLAY_DATA.TILE_INDEX);
            int spriteY = oamRamController.readAttr(spriteIndex, DISPLAY_DATA.Y_COORD) - SPRITE_YOFFSET;
            
            int[] tileLineBytes = videoRamController
                .tileLineBytes(spriteTileIndex, spriteTileLineIndex(lineIndex, spriteY, vFlip, spriteHeight));

            if (hFlip) {
                spriteLineBuilder.setBytes(0, tileLineBytes[1], tileLineBytes[0]);
            } else {
                spriteLineBuilder.setBytes(0, Bits.reverse8(tileLineBytes[1]), Bits.reverse8(tileLineBytes[0]));
            }

            spriteLines[i] = spriteLineBuilder.build()
                .shift(-spriteX).mapColors(lcdRegs.get(spritePalette ? LCDReg.OBP1 : LCDReg.OBP0));
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
    
    private int spriteTileLineIndex(int lineIndex, int spriteY, boolean vFlip, int spriteHeight) {
        int unflippedIndex = Math.floorMod(lineIndex - spriteY, spriteHeight);

        return vFlip ? spriteHeight - unflippedIndex : unflippedIndex;
    }
    
    private Integer[][] spriteLayerInfo(Integer[] spriteInfo) {
        List<Integer> bgSpriteInfo = new ArrayList<Integer>(SPRITES_PER_LINE);
        List<Integer> fgSpriteInfo = new ArrayList<Integer>(SPRITES_PER_LINE);
        
        for (int i = 0; i < spriteInfo.length; ++i) {
            boolean isInBG = oamRamController.readAttr(unpackIndex(spriteInfo[i]), ATTRIBUTES.BEHIND_BG);
            if (isInBG) {
                bgSpriteInfo.add(spriteInfo[i]);
            } else {
                fgSpriteInfo.add(spriteInfo[i]);
            }
        }
        
        return new Integer[][] { bgSpriteInfo.toArray(new Integer[0]), fgSpriteInfo.toArray(new Integer[0]) };
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
        return below.getOpacity().not().or(over.getOpacity());
    }
    
    private boolean isBackgroundActive() {
        return lcdRegs.testBit(LCDReg.LCDC, LCDC.BG);
    }

    private boolean isWindowActive(int adjustedWX) {
        return (adjustedWX >= 0 && adjustedWX < LCD_WIDTH && lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN));
    }
    
    private boolean areSpritesActive() {
        return lcdRegs.testBit(LCDReg.LCDC, LCDC.OBJ);
    }

    @Override
    public void attachTo(Bus bus) {
        Objects.requireNonNull(bus, "You must provide LcdController with a non-null bus");
        bus.attach(this);
        dmaController.setBus(bus);
    }

    @Override
    public int read(int address) {
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

        if (Preconditions.checkBits16(address) >= AddressMap.VRAM_START && address < AddressMap.VRAM_END) {
            videoRamController.write(address, data);
        } else if (address >= AddressMap.OAM_START && address < AddressMap.OAM_END) {
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
            default:
                lcdRegs.set(address - AddressMap.REGS_LCD_START, data);
            }
        }
    }

    private void modifyLYorLYC(LCDReg reg, int data) {
        Preconditions.checkArgument(reg == LCDReg.LY || reg == LCDReg.LYC);

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
