package ch.epfl.gameboj.component.lcd;

import static ch.epfl.gameboj.component.lcd.LcdImageLine.BLANK_LCD_IMAGE_LINE;
import static ch.epfl.gameboj.component.lcd.LcdImage.BLANK_LCD_IMAGE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;

public final class LcdController implements Component, Clocked {

    private enum LCDReg implements Register { LCDC, STAT, SCY, SCX, LY, LYC, DMA, BGP, OBP0, OBP1, WY, WX }

    private enum LCDC implements Bit { BG, OBJ, OBJ_SIZE, BG_AREA, TILE_SOURCE, WIN, WIN_AREA, LCD_STATUS } 

    private enum STAT implements Bit { MODE0, MODE1, LYC_EQ_LY, INT_MODE0, INT_MODE1, INT_MODE2, INT_LYC, UNUSED7 }
    
    private enum SPRITE_ATTR { Y_COORD, X_COORD, TILE_INDEX, MISC }
    
    private enum MISC implements Bit { P_NUM0, P_NUM1, P_NUM2, VRAM_BANK, PALETTE, FLIP_H, FLIP_V, BEHIND_BG }

    private static final int TILE_SIZE = 8;
    //Background size: 256 x 256, 32 x 32 tiles
    private static final int BG_SIZE = 256;
    private static final int BG_TILE_SIZE = BG_SIZE / TILE_SIZE;
    //Resolution: 160 x 144, 20 x 18 tiles
    public static final int LCD_WIDTH = 160, LCD_HEIGHT = 144;
    private static final int WIN_SIZE = BG_SIZE, WIN_TILE_SIZE = BG_TILE_SIZE;
    private static final int SPRITE_XOFFSET = 8, SPRITE_YOFFSET = 16;
    //Max sprites: 40 per screen, 10 per line
    private static final int MAX_SPRITES = 10, OAM_SPRITES = 40;
    private static final int SPRITE_ATTR_BYTES = 4;
    private static final int TILE_BYTE_LENGTH = 16;
    private static final int WX_OFFSET = 7;
    //Cycle durations
    private static final int MODE2_DURATION = 20, MODE3_DURATION = 43, MODE0_DURATION = 51;
    public static final int LINE_CYCLE_DURATION = MODE2_DURATION + MODE3_DURATION + MODE0_DURATION;
    public static final int IMAGE_CYCLE_DURATION = 154 * LINE_CYCLE_DURATION;
    private static final long END_LINE_DRAW = 16416; //TODO 114*144 + 1?
    private static final long IMAGE_DRAW = IMAGE_CYCLE_DURATION; //XXX
    private LcdImage displayedImage = BLANK_LCD_IMAGE;
    private final RamController videoRamController, oamRamController;
    private final Cpu cpu;
    private Bus bus;
    private long nextNonIdleCycle = Long.MAX_VALUE;
    private boolean winActive = false;
    private int winY = 0;
    private LcdImage.Builder nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
    private final DmaController dmaController = DmaController.getQuickCopyUtil();
    private final RegisterFile<Register> lcdRegs = new RegisterFile<>(LCDReg.values());
    private long lcdOnCycle;
    
    long cycFromPowOn;
    long cycFromImg;
    int currentLine;
    long cycFromLn;
    
    private long cyc, prevCyc; //XXX

    //TODO can only access HRAM during quick copy
    
    /**
     * Construit un contrôleur LCD.
     * 
     * @param cpu
     * le CPU avec lequel le contrôleur interagit
     */
    public LcdController(Cpu cpu) {
        this.cpu = Objects.requireNonNull(cpu);
        
        videoRamController = new RamController(new Ram(AddressMap.VRAM_SIZE), AddressMap.VRAM_START);
        oamRamController = new RamController(new Ram(AddressMap.OAM_RAM_SIZE), AddressMap.OAM_START);
    }

    public LcdImage currentImage() {
        return Objects.requireNonNull(displayedImage, "Displayed image cannot be null");
    }

    @Override
    public void cycle(long cycle) {       
        if (nextNonIdleCycle == Long.MAX_VALUE && isOn()) {
            turnOn(cycle);
        }
        
        if (dmaController.isActive()) {
            dmaController.copy();
        }
        
        cyc = cycle;//XXX
        
        cycFromImg = Math.floorMod(cycFromPowOn, IMAGE_CYCLE_DURATION);
        cycFromLn = Math.floorMod(cycFromImg, LINE_CYCLE_DURATION);
        
        compareLYandLYC(); //TODO correct? 
        
        cycFromPowOn = cycle - lcdOnCycle; //TODO can move???
        
        if (cycle == nextNonIdleCycle && isOn()) {
            currentLine = (int) (cycFromImg / LINE_CYCLE_DURATION);
            reallyCycle(cycle);
        }       
    }

    private void reallyCycle(long cycle) {
        System.out.println("reallyCycle: cycFromImg " + cycFromImg + " cycFromLn " + cycFromLn + " nextNonIdleCycle " + nextNonIdleCycle);
        System.out.println("current cycle: " + cycle + " line index: " + currentLine + ", current mode: " + Bits.clip(2, lcdRegs.get(LCDReg.STAT)));
        if (currentLine <= 143) {
            if (cycFromLn == MODE2_DURATION) {
                //System.out.println("switch to mode 3, elapsed cycles: " + (cyc - prevCyc));
                nextImageBuilder.setLine(currentLine, computeLine(currentLine));
                nextNonIdleCycle += MODE3_DURATION;
                setMode(3);
            } else if (cycFromLn == MODE2_DURATION + MODE3_DURATION) {
                //System.out.println("switch to mode 0, elapsed cycles: " + (cyc - prevCyc));
                nextNonIdleCycle += MODE0_DURATION;
                setMode(0);
            } else if (cycFromLn == 0) {
                if (currentLine == 143) {
                    //System.out.println("switch to mode 1, elapsed cycles: " + (cyc - prevCyc));
                    displayedImage = nextImageBuilder.build();
                    lcdRegs.increment(LCDReg.LY);
                    nextNonIdleCycle += LINE_CYCLE_DURATION;
                    setMode(1);
                } else {
                    //System.out.println("switch to mode 2, elapsed cycles: " + (cyc - prevCyc));
                    lcdRegs.increment(LCDReg.LY);
                    nextNonIdleCycle += MODE2_DURATION;
                    setMode(2);
                }
            }
        } else {
            if (currentLine == 153) {
                //System.out.println("switch to mode 2 from VBLANK, elapsed cycles: " + (cyc - prevCyc));
                lcdRegs.set(LCDReg.LY, 0);
                nextNonIdleCycle += MODE2_DURATION;
                setMode(2);
            } else {
                //System.out.println("draw virtual line, elapsed cycles: " + (cyc - prevCyc));
                lcdRegs.increment(LCDReg.LY);
                nextNonIdleCycle += LINE_CYCLE_DURATION;
            }
        }
        
        prevCyc = cyc;//XXX
        
        //////////////////////////////////////////
        
        /*if (nextNonIdleCycle == IMAGE_DRAW) {
            nextNonIdleCycle = Long.MAX_VALUE;
            currentLine = 0;
            winY = 0;
            return;
        }

        if (nextNonIdleCycle >= END_LINE_DRAW) {
            if (nextNonIdleCycle == END_LINE_DRAW) {
                displayedImage = nextImageBuilder.build();
            }
            nextNonIdleCycle += LINE_CYCLE_DURATION;
            changeLy(currentLine + 1);
            setMode(1);

            return;
        }

        if (nextNonIdleCycle == 0) {
            nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
        }

        switch ((int) (nextNonIdleCycle % LINE_CYCLE_DURATION)) {

        case 0:
            nextNonIdleCycle += 20;
            changeLy(currentLine + 1);
            setMode(2);
            break;

        case 20:
            nextNonIdleCycle += 43;
            nextImageBuilder.setLine(currentLine, computeLine(lcdRegs.get(LCDReg.LY)));
            setMode(3);
            break;

        case 63:
            nextNonIdleCycle += 51;
            setMode(0);
            break;

        }*/
    }
    
    private void changeLy(int val) {
        modifyLYorLYC(LCDReg.LY, val);
    }
    
    private void compareLYandLYC() {
        lcdRegs.setBit(LCDReg.STAT, STAT.LYC_EQ_LY, false);
        
        if (lcdRegs.get(LCDReg.LY) == lcdRegs.get(LCDReg.LYC)) {
            lcdRegs.setBit(LCDReg.STAT, STAT.LYC_EQ_LY, true);
            //TODO request STAT interrupt if appropriate, clean up code
        }
    }
    
    private void turnOff() {
        //TODO power off only possible during VBLANK
        setMode(0);
        modifyLYorLYC(LCDReg.LY, 0);
        nextNonIdleCycle = Long.MAX_VALUE;
    }
    
    private void turnOn(long cycle) {
        lcdOnCycle = cycle;
        nextNonIdleCycle = cycle + MODE2_DURATION;
        //setMode(2);
        //nextNonIdleCycle = 0;
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

    private LcdImageLine computeLine(int lineIndex) {  
        LcdImageLine nextLine = BLANK_LCD_IMAGE_LINE, bgSpriteLine = BLANK_LCD_IMAGE_LINE, fgSpriteLine = BLANK_LCD_IMAGE_LINE;
        
        int adjustedWX = lcdRegs.get(LCDReg.WX) - WX_OFFSET;
        
        if (lcdRegs.testBit(LCDReg.LCDC, LCDC.OBJ)) {
            List<Integer> bgSpriteInfoL = new ArrayList<>(10);
            List<Integer> fgSpriteInfoL = new ArrayList<>(10);
            Integer[] spriteInfo = spritesIntersectingLine(lineIndex);
            
            for (int i = 0; i < spriteInfo.length; ++i) {
                boolean isInBG = Bits.test(readAttr(i, SPRITE_ATTR.MISC), MISC.BEHIND_BG);
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
            LcdImageLine nextBGLine = computeBGLine(lineIndex);
            nextLine = nextLine.below(nextBGLine, computeMeldOpacity(nextLine, nextBGLine));
        }
        
        if (lineIndex >= lcdRegs.get(LCDReg.WY) && winActive) {
            nextLine = nextLine.join(computeWinLine(adjustedWX), adjustedWX);
        }
        
        nextLine = nextLine.below(fgSpriteLine);
        
        return nextLine;
    }
    
    private LcdImageLine computeBGLine(int lineIndex) {
        LcdImageLine.Builder nextBGLineBuilder = new LcdImageLine.Builder(BG_SIZE);
        
        for (int i = 0; i < BG_TILE_SIZE; ++i) {
            int bgAddress;

            int bgI = Math.floorDiv(Math.floorMod(lcdRegs.get(LCDReg.SCY) + lineIndex, BG_SIZE), TILE_SIZE) * BG_TILE_SIZE + i;

            if (lcdRegs.testBit(LCDReg.LCDC, LCDC.BG_AREA)) {
                bgAddress = AddressMap.BG_DISPLAY_DATA[1] + bgI;
            } else {
                bgAddress = AddressMap.BG_DISPLAY_DATA[0] + bgI;
            }

            int bgTypeIndex = read(bgAddress);

            nextBGLineBuilder.setBytes(i * Byte.SIZE, Bits.reverse8(tileLineMSB(bgTypeIndex, lineIndex)),
                    Bits.reverse8(tileLineLSB(bgTypeIndex, lineIndex)));
        }

        return nextBGLineBuilder.build().extractWrapped(lcdRegs.get(LCDReg.SCX), LCD_WIDTH).mapColors(lcdRegs.get(LCDReg.BGP));
    }
           
    private LcdImageLine computeWinLine(int adjustedWX) {
        LcdImageLine.Builder nextWinLineBuilder = new LcdImageLine.Builder(WIN_SIZE);
        
        winY++;
          
        for (int i = 0; i < WIN_TILE_SIZE; ++i) {
            int winAddress;

            int winI = Math.floorDiv(winY, TILE_SIZE) * WIN_TILE_SIZE + i;

            if (lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN_AREA)) {
                winAddress = AddressMap.BG_DISPLAY_DATA[1] + winI;
            } else {
                winAddress = AddressMap.BG_DISPLAY_DATA[0] + winI;
            }

            int winTypeIndex = read(winAddress);

            nextWinLineBuilder.setBytes(i * Byte.SIZE, Bits.reverse8(tileLineMSB(winTypeIndex, winY)), Bits.reverse8(tileLineLSB(winTypeIndex, winY)));
        }
        
        return nextWinLineBuilder.build().shift(-adjustedWX).extractWrapped(0, LCD_WIDTH).mapColors(lcdRegs.get(LCDReg.BGP));
    }
    
    private LcdImageLine computeSpriteLine(Integer[] spriteInfo, int lineIndex) { 
        LcdImageLine[] spriteLines = new LcdImageLine[spriteInfo.length];
        LcdImageLine spriteLine = BLANK_LCD_IMAGE_LINE;
        
        for (int i = 0; i < spriteInfo.length; ++i) {
            LcdImageLine.Builder spriteLineBuilder = new LcdImageLine.Builder(LCD_WIDTH);
            LcdImageLine indSpriteLine;
            int spriteIndex = unpackIndex(spriteInfo[i]);
            int spriteX = unpackX(spriteInfo[i]) - SPRITE_XOFFSET;
            int spriteAttrMisc = readAttr(spriteIndex, SPRITE_ATTR.MISC);
            boolean spritePalette = Bits.test(spriteAttrMisc, MISC.PALETTE.index());
            boolean hFlip = Bits.test(spriteAttrMisc, MISC.FLIP_H.index());
            boolean vFlip = Bits.test(spriteAttrMisc, MISC.FLIP_V.index());
            int spriteTileIndex = readAttr(spriteIndex, SPRITE_ATTR.TILE_INDEX);
            
            if (hFlip) {
                spriteLineBuilder.setBytes(0, tileLineMSB(spriteTileIndex, spriteIndex, lineIndex, vFlip),
                        tileLineLSB(spriteTileIndex, spriteIndex, lineIndex, vFlip));
            } else {
                spriteLineBuilder.setBytes(0, Bits.reverse8(tileLineMSB(spriteTileIndex, spriteIndex, lineIndex, vFlip)), 
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
        return read(tileTypeAddressS(tileTypeIndex, tileIndex, lineIndex, vFlipped) + 1);  //TODO FIGURE OUT -SPRITEY
    }
      
    private int tileLineLSB(int tileTypeIndex, int lineIndex) {
        return read(tileTypeAddress(tileTypeIndex, lineIndex));
    }
    
    private int tileLineLSB(int tileTypeIndex, int tileIndex, int lineIndex, boolean vFlipped) {
        return read(tileTypeAddressS(tileTypeIndex, tileIndex, lineIndex, vFlipped));  //TODO FIGURE OUT -SPRITEY
    }
        
    private int tileTypeAddress(int tileTypeIndex, int lineIndex) {
        if (lcdRegs.testBit(LCDReg.LCDC, LCDC.TILE_SOURCE)) {
            return AddressMap.TILE_SOURCE[1] + tileTypeIndex * TILE_BYTE_LENGTH + Math.floorMod(lineIndex, TILE_SIZE) * 2;
        } else {
            if (tileTypeIndex >= 0 && tileTypeIndex < 128) {
                return AddressMap.TILE_SOURCE[0] + (tileTypeIndex + 128) * TILE_BYTE_LENGTH + Math.floorMod(lineIndex, TILE_SIZE) * 2;
            } else if (tileTypeIndex >= 128 && tileTypeIndex < 256) {
                return AddressMap.TILE_SOURCE[0] + (tileTypeIndex - 128) * TILE_BYTE_LENGTH + Math.floorMod(lineIndex, TILE_SIZE) * 2;
            } else {
                throw new IllegalArgumentException("tile_type_index wrong!");
            }
        }
    }
    
    private int tileTypeAddressS(int tileTypeIndex, int tileIndex, int lineIndex, boolean vFlipped) {
        int height = getHeight();
        int spriteY = read(AddressMap.OAM_START + tileIndex * 4);
        
        if (vFlipped) {
            return AddressMap.TILE_SOURCE[1] + tileTypeIndex * TILE_BYTE_LENGTH + (height - Math.floorMod(lineIndex - spriteY, height)) * 2;
        } else {
            return AddressMap.TILE_SOURCE[1] + tileTypeIndex * TILE_BYTE_LENGTH + Math.floorMod(lineIndex - spriteY, height) * 2;
        }
    }
    
    private Integer[] spritesIntersectingLine(int lineIndex) {
        int scanIndex = 0, foundSprites = 0;
        int spriteHeight = getHeight();
        
        Integer[] intersect = new Integer[MAX_SPRITES]; //TODO replace with list?
        
        while (foundSprites < MAX_SPRITES && scanIndex < OAM_SPRITES) {
            int spriteY = readAttr(scanIndex, SPRITE_ATTR.Y_COORD) - SPRITE_YOFFSET;
            if (lineIndex >= spriteY && lineIndex < spriteY + spriteHeight) {
                intersect[foundSprites] = packSpriteInfo(scanIndex);
                foundSprites++;
            }
            
            scanIndex++;
        }
        
        Integer[] intersectIndex = trimIntArray(intersect, foundSprites);
        
        Arrays.sort(intersectIndex); //TODO replace with System.sortarray call?
        
        /*if (cyc >= 30_000_000 + (2 * (1L << 20)) - 17556) {
            Integer[] intIndex = new Integer[intersectIndex.length];
            boolean[] flipVer = new boolean[intersectIndex.length];
            boolean[] flipHor = new boolean[intersectIndex.length];
            
            for(int i = 0; i < intIndex.length; ++i) {
                intIndex[i] = unpackIndex(intersectIndex[i]);
                flipVer[i] = Bits.test(readAttr(intIndex[i], SPRITE_ATTR.MISC), MISC.FLIP_V.index());
                flipHor[i] = Bits.test(readAttr(intIndex[i], SPRITE_ATTR.MISC), MISC.FLIP_H.index());
            }
            
            System.out.println("Line index: " + lcdRegs.get(LCDReg.LY) + " sprite indexes " + Arrays.toString(intIndex));
            System.out.println("Vertical flip: " + Arrays.toString(flipVer));
            System.out.println("Horizontal flip: " + Arrays.toString(flipHor));
        }*/ //XXX
        
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
        //Sprite size: 8 x 8 or 8 x 16
        return TILE_SIZE * (lcdRegs.testBit(LCDReg.LCDC, LCDC.OBJ_SIZE) ? 2 : 1);
    }
    
    private int packSpriteInfo(int spriteIndex) {
        return readAttr(spriteIndex, SPRITE_ATTR.X_COORD) << Byte.SIZE | spriteIndex;
    }
    
    private int unpackX(int spriteInfo) {
        return Bits.extract(spriteInfo, Byte.SIZE, Byte.SIZE);
    }
    
    private int unpackIndex(int spriteInfo) {
        return Bits.clip(Byte.SIZE, spriteInfo);
    }
    
    private int readAttr(int spriteIndex, SPRITE_ATTR attr) {
        return read(AddressMap.OAM_START + Objects.checkIndex(spriteIndex, OAM_SPRITES) * SPRITE_ATTR_BYTES + attr.ordinal());
    }
    
    private BitVector computeMeldOpacity(LcdImageLine below, LcdImageLine over) {
        return below.getOpacity().and(over.getOpacity().not()).not();
    }
       
    private void toggleWindow(int lineIndex) {
        int adjustedWX = lcdRegs.get(LCDReg.WX) - WX_OFFSET;
        if (adjustedWX >= 0 && adjustedWX < 160 && lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN) && !winActive) {
            winActive = true;
        } else if (!(adjustedWX >= 0 && adjustedWX < 160 && lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN)) && winActive) {
            winY = lineIndex;
            winActive = false;
        }
    }
    
    @Override
    public void attachTo(Bus bus) {
        this.bus = bus;
        bus.attach(this);
        dmaController.setBus(bus);
    }

    @Override
    public int read(int address) {
        //TODO if drawing, cpu cannot access VRAM and OAM
        
        if (Preconditions.checkBits16(address) >= AddressMap.VRAM_START && address < AddressMap.VRAM_END) {
            return videoRamController.read(address);
        } else if (address >= AddressMap.OAM_START && address < AddressMap.OAM_END) {
            return oamRamController.read(address);
        } else if (address >= AddressMap.REGS_LCDC_START && address < AddressMap.REGS_LCDC_END) {
            return lcdRegs.get(address - AddressMap.REGS_LCDC_START);
        } 
        
        return NO_DATA;
    }

    @Override
    public void write(int address, int data) {
        Preconditions.checkBits8(data);
        
        //TODO if drawing, cpu cannot access VRAM and OAM

        if (Preconditions.checkBits16(address) >= AddressMap.VRAM_START && address < AddressMap.VRAM_END) {
            //TODO cannot access during mode 3
            videoRamController.write(address, data);
        } else if (address >= AddressMap.OAM_START && address < AddressMap.OAM_END) {
            //TODO cannot access during mode 2 or 3
            oamRamController.write(address, data);
        } else if (address >= AddressMap.REGS_LCDC_START && address < AddressMap.REGS_LCDC_END) {
            switch(address) {
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
                lcdRegs.set(LCDReg.WY, Objects.checkIndex(data, 145)); 
                break;
            case AddressMap.REG_WX:
                lcdRegs.set(LCDReg.WX, Objects.checkIndex(data, 167));
                break;
            }
                        
            toggleWindow(lcdRegs.get(LCDReg.LY)); //TODO does this work?          
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
