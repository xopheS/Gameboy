package ch.epfl.gameboj.component.lcd;

import static ch.epfl.gameboj.component.lcd.LcdImage.BLANK_LCD_IMAGE;
import static ch.epfl.gameboj.component.lcd.LcdImageLine.BLANK_LCD_IMAGE_LINE;
import static ch.epfl.gameboj.component.memory.OamRamController.MAX_SPRITES;
import static ch.epfl.gameboj.component.memory.OamRamController.SPRITES_PER_LINE;
import static ch.epfl.gameboj.component.memory.OamRamController.SPRITE_XOFFSET;
import static ch.epfl.gameboj.component.memory.OamRamController.SPRITE_YOFFSET;

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
import ch.epfl.gameboj.component.memory.OamRamController;
import ch.epfl.gameboj.component.memory.OamRamController.ATTRIBUTES;
import ch.epfl.gameboj.component.memory.OamRamController.DISPLAY_DATA;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;
import ch.epfl.gameboj.component.memory.VideoRamController;

/**
 * 
 * 
 * Classe qui représente l'écran de la GameBoy
 * 
 * @author Auguste Lefevre (269821)
 * @author Marc Watine (269508)
 */

public final class LcdController2 implements Clocked, Component {
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

    private static final int END_IMAGE_DRAW = 16416;

    public LcdController2(Cpu cpu) {
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
            //spriteBuilder.setLine(y, computeSpriteLine(oamRamController.spritesIntersectingLine(y, height), y));
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

        if (isOn() && (cycle - this.lcdOnCycle) == this.nextNonIdleCycle) {
            long cycFromPowOn = cycle - lcdOnCycle; 
            cycFromImg = (int) (cycFromPowOn % IMAGE_CYCLE_DURATION);
            currentImage = (int) Math.floorDiv(cycFromPowOn, IMAGE_CYCLE_DURATION);
            int currentLine = (int) (cycFromImg / LINE_CYCLE_DURATION);
            cycFromLn = cycFromImg % LINE_CYCLE_DURATION;
            reallyCycle(currentLine, (int) cycFromLn);
        }
    }

    /**
     * Méthode qui comporte les différents action à réaliser en fonction du
     * cycle (plus d'information dans la méthode)
     * 
     * @param cycle
     */
    private void reallyCycle(int currentLine, int cycFromLn) {

        // Cas ou les 144 lignes sont déssinés mais qu'il reste l'équivalent de
        // 10 lignes (en cycle soit 114*10) avant de passer au dessin de la
        // prochaine image
        if (this.nextNonIdleCycle % (IMAGE_CYCLE_DURATION) >= END_IMAGE_DRAW) {
            if (this.nextNonIdleCycle % (IMAGE_CYCLE_DURATION) == END_IMAGE_DRAW) {
                this.setMode(1);
                this.displayedImage = this.nextImageBuilder.build();
                this.winY = 0;
            }

            this.nextNonIdleCycle += LINE_CYCLE_DURATION;
            this.modifyLYorLYC(LCDReg.LY, currentLine);

            return;
        }

        // Cas ou l'image est complétement déssiné et prête a être afficher a
        // l'écran
        if (this.nextNonIdleCycle % (IMAGE_CYCLE_DURATION) == 0) {
            this.nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
        }

        switch (((int) this.nextNonIdleCycle % (IMAGE_CYCLE_DURATION))
                % LINE_CYCLE_DURATION) {

        case 0:
            this.nextNonIdleCycle += MODE2_DURATION;
            this.modifyLYorLYC(LCDReg.LY, currentLine);
            this.setMode(2);
            break;

        case MODE2_DURATION:
            this.nextNonIdleCycle += MODE3_DURATION;
            // System.out.println(register.get(Reg.LY));
            this.nextImageBuilder.setLine(lcdRegs.get(LCDReg.LY), this.computeLine(lcdRegs.get(LCDReg.LY)));
            this.setMode(3);
            break;

        case MODE2_DURATION + MODE3_DURATION:
            this.nextNonIdleCycle += MODE0_DURATION;
            this.setMode(0);
            break;

        }

    }
    
    private boolean isOn() {
        return lcdRegs.testBit(LCDReg.LCDC, LCDC.LCD_STATUS);
    }
    
    private void turnOff() {
        // TODO power off only possible during VBLANK
        setMode(0);
        modifyLYorLYC(LCDReg.LY, 0);
        nextNonIdleCycle = Long.MAX_VALUE;
    }
    
    private void turnOn(long cycle) {
        this.lcdOnCycle = cycle;
        this.nextNonIdleCycle = 0;
    }

    /**
     * Méthode qui permet de changer le mode/état du lcdControler et qui lance
     * les intéreputions nécessaire en fonctions de certaines conditions
     * 
     * @param mode
     *            la valeur du mode qu'on veut (entre 0 et 3 compris)
     */
    private void setMode(int mode) {
        Preconditions.checkArgument(mode >= 0 && mode <= 3);
        
        lcdRegs.setBit(LCDReg.STAT, STAT.MODE0, Bits.test(mode, 0));
        lcdRegs.setBit(LCDReg.STAT, STAT.MODE1, Bits.test(mode, 1));
        
        if (currentImage == 1) {
            System.out.println("cycles: " + cyc + " since frame: " + cycFromImg + " | mode: " + prevMode + " -> " + mode);
        }
        
        switch (mode) {
        case 0:
            if (lcdRegs.testBit(LCDReg.STAT, STAT.INT_MODE0))
                cpu.requestInterrupt(Interrupt.LCD_STAT);
            break;
        case 1:
            if (lcdRegs.testBit(LCDReg.STAT, STAT.INT_MODE1))
                cpu.requestInterrupt(Interrupt.LCD_STAT);
            cpu.requestInterrupt(Interrupt.VBLANK);
            break;
        case 2:
            if (lcdRegs.testBit(LCDReg.STAT, STAT.INT_MODE2))
                cpu.requestInterrupt(Interrupt.LCD_STAT);
            break;
        }
    }

    private LcdImageLine computeLine(int lineIndex) {
        LcdImageLine nextLine = BLANK_LCD_IMAGE_LINE, fgSpriteLine = BLANK_LCD_IMAGE_LINE;
        
        int adjustedWX = lcdRegs.get(LCDReg.WX) - WX_OFFSET;

        if (areSpritesActive()) {
            Integer[] spriteInfo = oamRamController.spritesIntersectingLine(lineIndex, getHeight());
            Integer[] spritesIndex = spritesIntersectingLine(lineIndex, getHeight());
            List<LcdImageLine> SpritesLine = computeSpriteLine(spriteInfo, lineIndex);
            
            LcdImageLine bgSpriteLine = BLANK_LCD_IMAGE_LINE;

            for (int i = spritesIndex.length - 1; i >= 0; --i) {
                boolean isBehind = oamRamController.readAttr(spritesIndex[i], ATTRIBUTES.BEHIND_BG);
                if (isBehind) {
                    bgSpriteLine = bgSpriteLine.below(SpritesLine.get(i));
                } else {
                    fgSpriteLine = fgSpriteLine.below(SpritesLine.get(i));
                }
            }
            
            nextLine = bgSpriteLine;
        }
        
        if (isBackgroundActive()) {
            LcdImageLine nextBGLine = computeBGLine(lineIndex).extractWrapped(lcdRegs.get(LCDReg.SCX), LCD_WIDTH);
            nextLine = nextLine.below(nextBGLine, computeMeldOpacity(nextLine, nextBGLine));
        }

        if (lineIndex >= lcdRegs.get(LCDReg.WY) && isWindowActive(adjustedWX)) {                                               
            nextLine = nextLine.join(computeWinLine(adjustedWX, winY, LCD_WIDTH), adjustedWX);
            winY++;
        }
        
        nextLine = nextLine.below(fgSpriteLine);
        
        return nextLine;
    }
    
    private LcdImageLine computeBGLine(int lineIndex) {
        LcdImageLine.Builder nextBGLineBuilder = new LcdImageLine.Builder(BG_SIZE);

        for (int i = 0; i < BG_TILE_SIZE; ++i) {
            int bgAddress;
            
            int tileIndex = (((lcdRegs.get(LCDReg.SCY) + lineIndex) % BG_SIZE) / TILE_SIZE) * BG_TILE_SIZE + i;
            
            if (Bits.test(lcdRegs.get(LCDReg.LCDC), LCDC.BG_AREA)) {
                bgAddress = AddressMap.BG_DISPLAY_DATA[1] + tileIndex;
            } else {
                bgAddress = AddressMap.BG_DISPLAY_DATA[0] + tileIndex;
            }
            
            int tileTypeIndex = videoRamController.read(bgAddress);
            
            nextBGLineBuilder.setBytes(Byte.SIZE * i,
                    Bits.reverse8(videoRamController.tileLineBytes(tileTypeIndex, bgTileLineIndex(lineIndex), lcdRegs.testBit(LCDReg.LCDC, LCDC.TILE_SOURCE), true)),
                    Bits.reverse8(videoRamController.tileLineBytes(tileTypeIndex, bgTileLineIndex(lineIndex), lcdRegs.testBit(LCDReg.LCDC, LCDC.TILE_SOURCE), false)));
        }

        return nextBGLineBuilder.build().mapColors(lcdRegs.get(LCDReg.BGP));
    }
    
    private LcdImageLine computeWinLine(int adjustedWX, int lineIndex, int width) {
        Preconditions.checkArgument(width % TILE_SIZE == 0, "The width must be a multiple of the tile size. Was provided: " + width);
        Objects.checkIndex(lineIndex, WIN_SIZE);
        
        LcdImageLine.Builder nextWinLineBuilder = new LcdImageLine.Builder(WIN_SIZE);
        
        adjustedWX = adjustedWX < 0 ? 0 : adjustedWX;
        
        for (int i = 0; i < WIN_TILE_SIZE; ++i) {
            int winAddress;
            
            int tileIndex = (lineIndex / TILE_SIZE) * WIN_TILE_SIZE + i;
            
            if (Bits.test(lcdRegs.get(LCDReg.LCDC), LCDC.WIN_AREA)) {
                winAddress = AddressMap.BG_DISPLAY_DATA[1] + tileIndex;
            } else {
                winAddress = AddressMap.BG_DISPLAY_DATA[0] + tileIndex;
            }
            
            int winTypeIndex = videoRamController.read(winAddress);

            int msb = videoRamController.tileLineBytes(winTypeIndex, winTileLineIndex(winY), lcdRegs.testBit(LCDReg.LCDC, LCDC.TILE_SOURCE), true);
            int lsb = videoRamController.tileLineBytes(winTypeIndex, winTileLineIndex(winY), lcdRegs.testBit(LCDReg.LCDC, LCDC.TILE_SOURCE), false);
            nextWinLineBuilder.setBytes(8 * i, Bits.reverse8(msb), Bits.reverse8(lsb));
        }
        
        return nextWinLineBuilder.build().mapColors(lcdRegs.get(LCDReg.BGP)).extractZeroExtended(-adjustedWX, width);
    }
    
    private List<LcdImageLine> computeSpriteLine(Integer[] spriteInfo, int lineIndex) {
        LcdImageLine[] spriteLines = new LcdImageLine[spriteInfo.length];
        LcdImageLine spriteLine = BLANK_LCD_IMAGE_LINE;
        List<LcdImageLine> listSpritesLine = new ArrayList<>();

        for (int i = 0; i < spriteInfo.length; ++i) {
            LcdImageLine.Builder spriteLineBuilder = new LcdImageLine.Builder(LCD_WIDTH);
            LcdImageLine indSpriteLine;
            int spriteX = unpackX(spriteInfo[i]) - SPRITE_XOFFSET;
            
            boolean spritePalette = oamRamController.readAttr(unpackIndex(spriteInfo[i]), ATTRIBUTES.PALETTE);
            boolean hFlip = oamRamController.readAttr(unpackIndex(spriteInfo[i]), ATTRIBUTES.FLIP_H);
            boolean vFlip = oamRamController.readAttr(unpackIndex(spriteInfo[i]), ATTRIBUTES.FLIP_V);
            int spriteTileIndex = oamRamController.readAttr(unpackIndex(spriteInfo[i]), DISPLAY_DATA.TILE_INDEX);
            int spriteY = oamRamController.readAttr(unpackIndex(spriteInfo[i]), DISPLAY_DATA.Y_COORD) - SPRITE_YOFFSET;
            
            if (hFlip) {
                spriteLineBuilder.setBytes(0,
                        videoRamController.tileLineBytes(spriteTileIndex, spriteTileLineIndex(lcdRegs.get(LCDReg.LY), spriteY, vFlip, getHeight()), true),
                        videoRamController.tileLineBytes(spriteTileIndex, spriteTileLineIndex(lcdRegs.get(LCDReg.LY), spriteY, vFlip, getHeight()), false));
            } else {
                spriteLineBuilder.setBytes(0,
                        Bits.reverse8(videoRamController.tileLineBytes(spriteTileIndex, spriteTileLineIndex(lcdRegs.get(LCDReg.LY), spriteY, vFlip, getHeight()), true)),
                        Bits.reverse8(videoRamController.tileLineBytes(spriteTileIndex, spriteTileLineIndex(lcdRegs.get(LCDReg.LY), spriteY, vFlip, getHeight()), false)));
            }

            LcdImageLine line = spriteLineBuilder.build().mapColors(spritePalette ? lcdRegs.get(LCDReg.OBP1) : lcdRegs.get(LCDReg.OBP0)).shift(-spriteX);

            listSpritesLine.add(line);
            spriteLines[i] = line;
        }
        
        for (int i = spriteInfo.length - 1; i >= 0; --i) {
            spriteLine = spriteLine.below(spriteLines[i]);
        }
        
        return listSpritesLine;
    }

    private Integer[] spritesIntersectingLine(int lineIndex, int height) {
        int scanIndex = 0, foundSprites = 0;
        
        Integer[] intersect = new Integer[SPRITES_PER_LINE];
        
        while (foundSprites < SPRITES_PER_LINE && scanIndex < MAX_SPRITES) {
            int spriteY = oamRamController.readAttr(scanIndex, DISPLAY_DATA.Y_COORD) - SPRITE_YOFFSET;
            
            if (lineIndex >= spriteY && lineIndex < spriteY + height) {
                intersect[foundSprites] = oamRamController.packSpriteInfo(scanIndex);
                foundSprites++;
            }
            
            scanIndex++;
        }
        
        Integer[] intersectIndex = oamRamController.trimIntArray(intersect, foundSprites);

        Integer[] finalTab = new Integer[foundSprites];
        for (int i = 0; i < foundSprites; ++i) {
            finalTab[i] = unpackIndex(intersect[i]);
        }
        
        Arrays.sort(intersectIndex);

        return finalTab;
    }

    private int bgTileLineIndex(int lineIndex) {
        return (lcdRegs.get(LCDReg.SCY) + Objects.checkIndex(lineIndex, BG_SIZE)) % TILE_SIZE;
    }
    
    private int winTileLineIndex(int lineIndex) {
        return Objects.checkIndex(lineIndex, WIN_SIZE) % TILE_SIZE;
    }
    
    private int spriteTileLineIndex(int lineIndex, int spriteY, boolean vFlip, int height) {
        if (vFlip) {
            return (height - Math.floorMod(lineIndex - spriteY, height));
        } else {
            return Math.floorMod(lineIndex - spriteY, height);
        }
    }
    
    private Integer[][] spriteLayerInfo(Integer[] spriteInfo) {
        List<Integer> bgSpriteInfo = new ArrayList<Integer>(10);
        List<Integer> fgSpriteInfo = new ArrayList<Integer>(10);
        
        for (int i = 0; i < spriteInfo.length; ++i) {
            boolean isInBG = oamRamController.readAttr(i, ATTRIBUTES.BEHIND_BG);
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
        return below.getOpacity().not().or(over.getOpacity()); //FIXME
    }
    
    private boolean isBackgroundActive() {
        return lcdRegs.testBit(LCDReg.LCDC, LCDC.BG);
    }

    private boolean isWindowActive(int adjustedWX) {
        return (adjustedWX >= 0 && adjustedWX < 160 && lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN));
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
                // Active la copie directe dans la mémoire OAM (spriteRam)
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
            System.out.println("cycles: " + cyc + " since frame: " + cycFromImg + " | LY: " + lcdRegs.get(LCDReg.LY) + " -> " + data);
        
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