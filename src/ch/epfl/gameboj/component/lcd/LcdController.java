package ch.epfl.gameboj.component.lcd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;

public final class LcdController implements Component, Clocked {

    private enum LCDReg implements Register { LCDC, STAT, SCY, SCX, LY, LYC, DMA, BGP, OBP0, OBP1, WY, WX }

    private enum LCDC implements Bit { BG, OBJ, OBJ_SIZE, BG_AREA, TILE_SOURCE, WIN, WIN_AREA, LCD_STATUS }

    private enum STAT implements Bit { MODE0, MODE1, LYC_EQ_LY, INT_MODE0, INT_MODE1, INT_MODE2, INT_LYC, UNUSED }
    
    private enum SPRITE_ATTR { Y_COORD, X_COORD, TILE_INDEX, MISC }
    
    private enum MISC implements Bit { UNUSED0, UNUSED1, UNUSED2, UNUSED3, PALETTE, FLIP_H, FLIP_V, BEHIND_BG }

    private static final int TILE_SIZE = 8;
    private static final int BG_SIZE = 256;
    private static final int BG_TILE_SIZE = BG_SIZE / TILE_SIZE;
    public static final int LCD_WIDTH = 160, LCD_HEIGHT = 144;
    private static final int LCD_TILE_WIDTH = LCD_WIDTH / TILE_SIZE;
    private static final int LCD_TILE_HEIGHT = LCD_HEIGHT / TILE_SIZE;
    private static final int WIN_WIDTH = LCD_WIDTH, WIN_TILE_WIDTH = LCD_TILE_WIDTH;
    private static final int WIN_HEIGHT = LCD_WIDTH, WIN_TILE_HEIGHT = LCD_TILE_WIDTH;
    private static final int MODE2_DURATION = 20, MODE3_DURATION = 43, MODE0_DURATION = 51;
    private static final int LINE_CYCLE_DURATION = MODE2_DURATION + MODE3_DURATION + MODE0_DURATION;
    private static final int SPRITE_XOFFSET = 8, SPRITE_YOFFSET = 16;
    private static final int MAX_SPRITES = 10;
    private static final int SPRITE_ATTR_BYTES = 4;
    private static final LcdImageLine EMPTY_LCD_LINE = new LcdImageLine(new BitVector(LCD_WIDTH), new BitVector(LCD_WIDTH), new BitVector(LCD_WIDTH));
    private static final LcdImageLine EMPTY_BG_LINE = new LcdImageLine(new BitVector(BG_SIZE), new BitVector(BG_SIZE), new BitVector(BG_SIZE));
    private LcdImage displayedImage;
    private final Ram videoRam, oamRam;
    private final Cpu cpu;
    private Bus bus;
    private long nextNonIdleCycle = Long.MAX_VALUE;
    private long lineStartT = 0;
    private boolean winActive = false;
    private int winY = 0;
    private int adjustedWX = -7;
    private LcdImage.Builder nextImageBuilder;
    private QuickCopyInfo quickCopy = new QuickCopyInfo();
    private final RegisterFile<Register> lcdRegs = new RegisterFile<>(LCDReg.values());
    
    //TODO SPRITE HEIGHT 
    //TODO fix sprites

    private static class QuickCopyInfo {
        boolean isActive = false;
        int startAddress;
        int currentIndex = 0;
        
        void setAddressMSB(int addressMSB) {
            startAddress = Preconditions.checkBits8(addressMSB) << 8;
        }
    }
    
    /**
     * Construit un contrôleur LCD.
     * 
     * @param cpu
     * le CPU avec lequel le contrôleur interagit
     */
    public LcdController(Cpu cpu) {

        this.cpu = Objects.requireNonNull(cpu);
        videoRam = new Ram(AddressMap.VIDEO_RAM_SIZE);
        oamRam = new Ram(AddressMap.OAM_RAM_SIZE);

        ArrayList<LcdImageLine> imgLines = new ArrayList<>(LCD_HEIGHT);

        for (int i = 0; i < LCD_HEIGHT; ++i) {
            imgLines.add(EMPTY_LCD_LINE);
        }

        displayedImage = new LcdImage(LCD_WIDTH, LCD_HEIGHT, imgLines);

        nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
    }

    public LcdImage currentImage() {
        return Objects.requireNonNull(displayedImage, "fatal: attempt to display a null image");
    }

    @Override
    public void cycle(long cycle) {
        if (quickCopy.isActive) {
            if (quickCopy.currentIndex < 159) {
                write(AddressMap.OAM_START + quickCopy.currentIndex, bus.read(quickCopy.startAddress + quickCopy.currentIndex));
                quickCopy.currentIndex++;
            } else {
                quickCopy = new QuickCopyInfo();
            }
            nextNonIdleCycle++; 
        } else if (nextNonIdleCycle == Long.MAX_VALUE && lcdRegs.testBit(LCDReg.LCDC, LCDC.LCD_STATUS)) {
            lineStartT = cycle;
            nextNonIdleCycle = cycle + 20;
            setMode(2);
            reallyCycle(cycle);
        } else if (cycle != nextNonIdleCycle || !lcdRegs.testBit(LCDReg.LCDC, LCDC.LCD_STATUS)) {
            return;
        } else {
            reallyCycle(cycle);
        }
    }

    private void reallyCycle(long cycle) {
        int mode0 = lcdRegs.testBit(LCDReg.STAT, STAT.MODE0) ? 1 : 0;
        int mode1 = lcdRegs.testBit(LCDReg.STAT, STAT.MODE1) ? 1 : 0;
        int mode = mode0 | (mode1 << 1);

        if (mode == 0 && lcdRegs.get(LCDReg.LY) == LCD_HEIGHT - 1) {
            displayedImage = nextImageBuilder.build();
            modifyLYorLYC(LCDReg.LY, lcdRegs.get(LCDReg.LY) + 1);
            setMode(1);
            lineStartT = cycle;
            nextNonIdleCycle += 114;
        } else if (mode == 2 && cycle - lineStartT == MODE2_DURATION) {
            nextImageBuilder.setLine(lcdRegs.get(LCDReg.LY), computeLine());
            setMode(3);
            nextNonIdleCycle += 43;
        } else if (mode == 3 && cycle - lineStartT == MODE2_DURATION + MODE3_DURATION) {
            setMode(0);
            nextNonIdleCycle += 51;
        } else if (mode == 0 && cycle - lineStartT == MODE2_DURATION + MODE3_DURATION + MODE0_DURATION) {
            modifyLYorLYC(LCDReg.LY, lcdRegs.get(LCDReg.LY) + 1);
            lineStartT = cycle;
            setMode(2);
            nextNonIdleCycle += 20;
        } else if (mode == 1 && cycle - lineStartT == LINE_CYCLE_DURATION) {
            lineStartT = cycle;
            if (lcdRegs.get(LCDReg.LY) == 153) {
                nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_WIDTH);
                modifyLYorLYC(LCDReg.LY, 0);
                setMode(2);
                winY = 0;
                nextNonIdleCycle += 20;
            } else {
                modifyLYorLYC(LCDReg.LY, lcdRegs.get(LCDReg.LY) + 1);
                nextNonIdleCycle += 114;
            }
        }
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
                cpu.requestInterrupt(Interrupt.VBLANK);
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

    private LcdImageLine computeLine() {  
        LcdImageLine nextLine;
        int lineIndex = lcdRegs.get(LCDReg.LY);
        
        if (lcdRegs.testBit(LCDReg.LCDC, LCDC.OBJ)) {
            List<Integer> bgSpriteIndex = new ArrayList<>(10);
            List<Integer> fgSpriteIndex = new ArrayList<>(10);
            Integer[] spritesIntersectingLine = spritesIntersectingLine(lineIndex);
            
            for (int i = 0; i < spritesIntersectingLine.length; ++i) {
                boolean isInBG = Bits.test(read(AddressMap.OAM_START + i * SPRITE_ATTR_BYTES + SPRITE_ATTR.MISC.ordinal()), MISC.BEHIND_BG);
                if (isInBG) {
                    bgSpriteIndex.add(spritesIntersectingLine[i]);
                } else {
                    fgSpriteIndex.add(spritesIntersectingLine[i]);
                }
            }
            
            Integer[] bgSpriteIndexes = bgSpriteIndex.toArray(new Integer[0]);
            Integer[] fgSpriteIndexes = fgSpriteIndex.toArray(new Integer[0]);
            
            nextLine = computeSpriteLine(bgSpriteIndexes);
            
            if (lcdRegs.testBit(LCDReg.LCDC, LCDC.BG)) {
                LcdImageLine nextBGLine = computeBGLine();
                nextLine = nextLine.below(nextBGLine, computeMeldOpacity(nextLine, nextBGLine));
                
                if (lineIndex >= lcdRegs.get(LCDReg.WY) && winActive) {
                    nextLine = nextLine.join(computeWinLine(), adjustedWX);
                }
            } else { 
                nextLine = EMPTY_LCD_LINE;
                
                if (lineIndex >= lcdRegs.get(LCDReg.WY) && winActive) {
                    nextLine = nextLine.join(computeWinLine(), adjustedWX);
                }
            }
            
            nextLine = nextLine.below(computeSpriteLine(fgSpriteIndexes));
        } else {
            if (lcdRegs.testBit(LCDReg.LCDC, LCDC.BG)) {
                nextLine = computeBGLine();
                
                if (lineIndex >= lcdRegs.get(LCDReg.WY) && winActive) {
                    nextLine = nextLine.join(computeWinLine(), adjustedWX);
                }
            } else { 
                nextLine = EMPTY_LCD_LINE;
                
                if (lineIndex >= lcdRegs.get(LCDReg.WY) && winActive) {
                    nextLine = nextLine.join(computeWinLine(), adjustedWX);
                }
            }
        }
        
        return nextLine;
    }
    
    private LcdImageLine computeBGLine() {
        LcdImageLine.Builder nextBGLineBuilder = new LcdImageLine.Builder(BG_SIZE);
        
        for (int i = 0; i < BG_TILE_SIZE; ++i) {
            int bgAddress;

            int bgI = Math.floorDiv(Math.floorMod(lcdRegs.get(LCDReg.SCY) + lcdRegs.get(LCDReg.LY), BG_SIZE), TILE_SIZE) * BG_TILE_SIZE + i;

            if (lcdRegs.testBit(LCDReg.LCDC, LCDC.BG_AREA)) {
                bgAddress = AddressMap.BG_DISPLAY_DATA[1] + bgI;
            } else {
                bgAddress = AddressMap.BG_DISPLAY_DATA[0] + bgI;
            }

            int bgTypeIndex = read(bgAddress);

            nextBGLineBuilder.setBytes(i * Byte.SIZE, Bits.reverse8(tileLineMSB(bgTypeIndex, lcdRegs.get(LCDReg.LY), false)),
                    Bits.reverse8(tileLineLSB(bgTypeIndex, lcdRegs.get(LCDReg.LY), false)));
        }

        return nextBGLineBuilder.build().extractWrapped(lcdRegs.get(LCDReg.SCX), LCD_WIDTH).mapColors(lcdRegs.get(LCDReg.BGP));
    }
           
    private LcdImageLine computeWinLine() {
        LcdImageLine.Builder nextWinLineBuilder = new LcdImageLine.Builder(BG_SIZE);
        
        winY++;
          
        for (int i = 0; i < WIN_TILE_WIDTH; ++i) {
            int winAddress;

            int winI = Math.floorDiv(winY, TILE_SIZE) * WIN_TILE_WIDTH + i;

            if (lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN_AREA)) {
                winAddress = AddressMap.BG_DISPLAY_DATA[1] + winI;
            } else {
                winAddress = AddressMap.BG_DISPLAY_DATA[0] + winI;
            }

            int winTypeIndex = read(winAddress);

            nextWinLineBuilder.setBytes(i * Byte.SIZE, Bits.reverse8(tileLineMSB(winTypeIndex, winY, false)), Bits.reverse8(tileLineLSB(winTypeIndex, winY, false)));
        }
        
        return nextWinLineBuilder.build().shift(-adjustedWX).extractWrapped(0, LCD_WIDTH).mapColors(lcdRegs.get(LCDReg.BGP));
    }
    
    private LcdImageLine computeSpriteLine(Integer[] spriteIndexes) { 
        LcdImageLine[] spriteLines = new LcdImageLine[spriteIndexes.length];
        LcdImageLine spriteLine = EMPTY_LCD_LINE;
        
        for (int i = 0; i < spriteIndexes.length; ++i) {
            LcdImageLine.Builder spriteLineBuilder = new LcdImageLine.Builder(LCD_WIDTH);
            LcdImageLine indSpriteLine;
            boolean spritePalette = Bits.test(read(AddressMap.OAM_START + spriteIndexes[i] * 4 + SPRITE_ATTR.MISC.ordinal()), MISC.PALETTE.index());
            int spriteTileIndex = read(AddressMap.OAM_START + spriteIndexes[i] * SPRITE_ATTR_BYTES + SPRITE_ATTR.TILE_INDEX.ordinal());
            int spriteX = read(AddressMap.OAM_START + spriteIndexes[i] * SPRITE_ATTR_BYTES + SPRITE_ATTR.X_COORD.ordinal()) - SPRITE_XOFFSET;
            
            spriteLineBuilder.setBytes(0, tileLineMSB(spriteTileIndex, lcdRegs.get(LCDReg.LY), true), tileLineLSB(spriteTileIndex, lcdRegs.get(LCDReg.LY), true));
            
            indSpriteLine = spriteLineBuilder.build().shift(-spriteX);
            
            if (spritePalette) {
                indSpriteLine.mapColors(lcdRegs.get(LCDReg.OBP1));
            } else {
                indSpriteLine.mapColors(lcdRegs.get(LCDReg.OBP0));
            }
            
            spriteLines[i] = indSpriteLine;
        }
        
        for (int i = 0; i < spriteLines.length; ++i) {
            spriteLine = spriteLine.below(spriteLines[i]);
        }
        
        return spriteLine;
    }
    
    private int tileLineMSB(int tileTypeIndex, int lineIndex, boolean isSprite) {
        return read(tileTypeAddress(tileTypeIndex, lineIndex, isSprite) + 1);
    }
    
    private int tileLineLSB(int tileTypeIndex, int lineIndex, boolean isSprite) {
        return read(tileTypeAddress(tileTypeIndex, lineIndex, isSprite));
    }
    
    private int tileTypeAddress(int tileTypeIndex, int lineIndex, boolean isSprite) {
        if (isSprite || lcdRegs.testBit(LCDReg.LCDC, LCDC.TILE_SOURCE)) {
            return AddressMap.TILE_SOURCE[1] + tileTypeIndex * 16 + Math.floorMod(lineIndex, TILE_SIZE) * 2;
        } else {
            if (tileTypeIndex >= 0 && tileTypeIndex < 128) {
                return AddressMap.TILE_SOURCE[0] + (tileTypeIndex + 128) * 16 + Math.floorMod(lineIndex, TILE_SIZE) * 2;
            } else if (tileTypeIndex >= 128 && tileTypeIndex < 256) {
                return AddressMap.TILE_SOURCE[0] + (tileTypeIndex - 128) * 16 + Math.floorMod(lineIndex, TILE_SIZE) * 2;
            } else {
                throw new IllegalArgumentException("tile_type_index wrong!");
            }
        }
    }
    
    private Integer[] spritesIntersectingLine(int lineIndex) {
        int scanIndex = 0, foundSprites = 0;
        int spriteHeight = lcdRegs.testBit(LCDReg.LCDC, LCDC.OBJ_SIZE) ? 2 * TILE_SIZE : TILE_SIZE;
        
        Integer[] intersect = new Integer[10];
        
        while (foundSprites < 10 && scanIndex < 40) {
            int spriteY = read(scanIndex * SPRITE_ATTR_BYTES + SPRITE_ATTR.Y_COORD.ordinal()) - SPRITE_YOFFSET;
            if (lcdRegs.get(LCDReg.LY) >= spriteY && lcdRegs.get(LCDReg.LY) < spriteY + spriteHeight) {
                foundSprites++;
                intersect[foundSprites] = read(AddressMap.OAM_START + scanIndex * 4 + SPRITE_ATTR.X_COORD.ordinal()) << Byte.SIZE | 
                        read(AddressMap.OAM_START + scanIndex * 4 + SPRITE_ATTR.TILE_INDEX.ordinal());
            }
            
            scanIndex++;
        }
        
        Integer[] intersectIndex = new Integer[foundSprites];
        
        for (int i = 0; i < foundSprites; ++i) {
            intersectIndex[i] = intersect[i];
        }
        
        Arrays.sort(intersectIndex, Comparator.comparingInt((Integer index) -> Bits.extract(intersectIndex[index], 8, 8) - SPRITE_XOFFSET)
                .thenComparing(Comparator.comparingInt((Integer index) -> Bits.clip(8, intersectIndex[index]))));
        
        return intersectIndex;
    }

    private BitVector computeMeldOpacity(LcdImageLine below, LcdImageLine over) {
        return below.getOpacity().and(over.getOpacity().not()).not();
    }
    
    @Override
    public void attachTo(Bus bus) {
        this.bus = bus;
        bus.attach(this);
    }

    @Override
    public int read(int address) throws IllegalArgumentException {
        if (Preconditions.checkBits16(address) >= AddressMap.VIDEO_RAM_START && address < AddressMap.VIDEO_RAM_END) {
            return videoRam.read(address - AddressMap.VIDEO_RAM_START);
        } else if (address >= AddressMap.OAM_START && address < AddressMap.OAM_END) {
            return oamRam.read(address - AddressMap.OAM_START);
        } else if (address >= AddressMap.REGS_LCDC_START && address < AddressMap.REGS_LCDC_END) {
            return lcdRegs.get(address - AddressMap.REGS_LCDC_START);
        } else {
            return NO_DATA;
        }
    }

    @Override
    public void write(int address, int data) throws IllegalArgumentException {

        Preconditions.checkBits8(data);

        if (Preconditions.checkBits16(address) >= AddressMap.VIDEO_RAM_START && address < AddressMap.VIDEO_RAM_END) {
            videoRam.write(address - AddressMap.VIDEO_RAM_START, data);
        }

        if (address >= AddressMap.REGS_LCDC_START && address < AddressMap.REGS_LCDC_END) {
            if (address == AddressMap.REGS_LCDC_START + LCDReg.LCDC.index()) {
                lcdRegs.set(LCDReg.LCDC, data);
                if (!lcdRegs.testBit(LCDReg.LCDC, LCDC.LCD_STATUS)) {
                    setMode(0);
                    modifyLYorLYC(LCDReg.LY, 0);
                    nextNonIdleCycle = Long.MAX_VALUE;
                }
                if (lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN) && !winActive) {
                    winActive = true;
                } else if (!lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN) && winActive) {
                    winY = lcdRegs.get(LCDReg.LY);
                    winActive = false;
                }
            } else if (address == AddressMap.REGS_LCDC_START + LCDReg.STAT.index()) {
                lcdRegs.set(LCDReg.STAT, data & 0b1111_1000 | lcdRegs.get(LCDReg.STAT) & 0b0000_0111);
            } else if (address == AddressMap.REGS_LCDC_START + LCDReg.LY.index()) {
                modifyLYorLYC(LCDReg.LY, data);
            } else if (address == AddressMap.REGS_LCDC_START + LCDReg.LYC.index()) {
                modifyLYorLYC(LCDReg.LYC, data);
            } else if (address == AddressMap.REGS_LCDC_START + LCDReg.DMA.index()) {
                quickCopy.isActive = true;
                quickCopy.setAddressMSB(data);
            } else if (address == AddressMap.REGS_LCDC_START + LCDReg.WX.index()) {
                lcdRegs.set(LCDReg.WX, data);
                adjustedWX = lcdRegs.get(LCDReg.WX) - 7;
                if (adjustedWX >= 0 && adjustedWX < 16 && !winActive) {
                    winActive = true;
                } else if (!(adjustedWX >= 0 && adjustedWX < 16) && winActive) {
                    winY = lcdRegs.get(LCDReg.LY);
                    winActive = false;
                }
            } else {
                lcdRegs.set(address - AddressMap.REGS_LCDC_START, data);
            }
        }
    }

    private void modifyLYorLYC(LCDReg reg, int data) {
        Preconditions.checkArgument(reg == LCDReg.LY || reg == LCDReg.LYC);

        lcdRegs.set(reg, data);

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
