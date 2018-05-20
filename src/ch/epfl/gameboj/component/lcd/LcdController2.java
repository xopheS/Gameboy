package ch.epfl.gameboj.component.lcd;

import static ch.epfl.gameboj.component.lcd.LcdImage.BLANK_LCD_IMAGE;
import static ch.epfl.gameboj.component.lcd.LcdImageLine.BLANK_LCD_IMAGE_LINE;
import static ch.epfl.gameboj.component.memory.OamRamController.SPRITE_XOFFSET;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    
    private static final int TILE_LINE = 8;
    // Background size: 256 x 256, 32 x 32 tiles
    private static final int BG_SIZE = 256;
    private static final int BG_TILE_SIZE = BG_SIZE / TILE_LINE;
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

    private static final int TILE_SIZE = 16;
    private static final int IMAGE_DRAW = 17556;
    private static final int LINE_DRAW_CYCLE = 114;
    private static final int END_IMAGE_DRAW = 16416;
    private static final int TILE_SOURCE_NUMBER = 128;
    
    private static final int WX_OFF = 7;
    private static final int SPRITE_INFO = 4;
    private static final int SPRITEY_OFF = 16;
    private static final int SPRITEX_OFF = 8;
    private static final int MAX_SPRITE = 10;
    private static final int NUMBER_SPRITE = 40;
    private static final BitVector EMPTY_VECTOR = new BitVector(LCD_WIDTH,
            false);
    private static final LcdImageLine EMPTY_LINE = new LcdImageLine(
            EMPTY_VECTOR, EMPTY_VECTOR, EMPTY_VECTOR);
    private static final LcdImage EMPTY_IMAGE = new LcdImage(LCD_WIDTH,
            LCD_HEIGHT, new ArrayList<LcdImageLine>(
                    Collections.nCopies(LCD_HEIGHT, EMPTY_LINE)));

    private Bus bus;
    private int indexLine = 0;
    private boolean copyActive = false;
    private int counterOfCopy = 0;

    /**
     * Enumaration représentant les différents bit du de l'octet INFO d'un
     * sprite (cf. SPRITE enum)
     *
     */
    private enum SINFO implements Bit {
        NULL0, NULL1, NULL2, NULL3, PALETTE, FLIP_H, FLIP_V, BEHIND_BG
    }

    /**
     * Constructeur publique du LcdController permettant de créer la ramVideo et
     * la ram OAM ainsi que définir le cpu lié à cet écran
     * 
     * @param cpu
     *            le cpu lié a l'écran
     */
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
            //backgroundBuilder.setLine(y, computeBGLine(y));
        }
        return backgroundBuilder.build();
    }

    public LcdImage getWindow() {
        LcdImage.Builder windowBuilder = new LcdImage.Builder(WIN_SIZE, WIN_SIZE);
        for (int y = 0; y < WIN_SIZE; ++y) {
            //windowBuilder.setLine(y, computeWinLine(0, y, WIN_SIZE));
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

        if (this.copyActive && this.counterOfCopy < 160) {
            this.write(AddressMap.OAM_START + this.counterOfCopy, bus.read((lcdRegs.get(LCDReg.DMA) << 8) + this.counterOfCopy));
            this.counterOfCopy += 1;
            if (this.counterOfCopy == 160) {
                this.copyActive = false;
                this.counterOfCopy = 0;
            }
        }

        if (isOn() && (cycle - this.lcdOnCycle) == this.nextNonIdleCycle) {
            indexLine = ((((int) ((cycle - this.lcdOnCycle))) % IMAGE_DRAW)
                    / LINE_DRAW_CYCLE);
            this.reallyCycle();
        }
    }

    /**
     * Méthode qui comporte les différents action à réaliser en fonction du
     * cycle (plus d'information dans la méthode)
     * 
     * @param cycle
     */
    private void reallyCycle() {

        // Cas ou les 144 lignes sont déssinés mais qu'il reste l'équivalent de
        // 10 lignes (en cycle soit 114*10) avant de passer au dessin de la
        // prochaine image
        if (this.nextNonIdleCycle % (IMAGE_DRAW) >= END_IMAGE_DRAW) {
            if (this.nextNonIdleCycle % (IMAGE_DRAW) == END_IMAGE_DRAW) {
                this.setMode(1);
                this.displayedImage = this.nextImageBuilder.build();
                this.winY = 0;
            }

            this.nextNonIdleCycle += LINE_DRAW_CYCLE;
            this.modifyLYorLYC(LCDReg.LY, this.indexLine);

            return;
        }

        // Cas ou l'image est complétement déssiné et prête a être afficher a
        // l'écran
        if (this.nextNonIdleCycle % (IMAGE_DRAW) == 0) {
            this.nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
        }

        switch (((int) this.nextNonIdleCycle % (IMAGE_DRAW))
                % LINE_DRAW_CYCLE) {

        case 0:
            this.nextNonIdleCycle += MODE2_DURATION;
            this.modifyLYorLYC(LCDReg.LY, this.indexLine);
            this.setMode(2);
            break;

        case MODE2_DURATION:
            this.nextNonIdleCycle += MODE3_DURATION;
            // System.out.println(register.get(Reg.LY));
            this.nextImageBuilder.setLine(lcdRegs.get(LCDReg.LY),
                    this.computeLine(lcdRegs.get(LCDReg.LY)));
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
     * @param value
     *            la valeur du mode qu'on veut (entre 0 et 3 compris)
     */
    private void setMode(int value) {
        Preconditions.checkArgument(value >= 0 && value <= 3);
        
        lcdRegs.setBit(LCDReg.STAT, STAT.MODE0, Bits.test(value, 0));
        lcdRegs.setBit(LCDReg.STAT, STAT.MODE1, Bits.test(value, 1));
        
        switch (value) {
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

    private void setRegValue(int address, int data) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);
        switch (address) {
        case AddressMap.REG_LCDC:
            lcdRegs.set(LCDReg.LCDC, data);
            break;
        case AddressMap.REG_LCD_STAT:
            lcdRegs.set(LCDReg.STAT, data);
            break;
        case AddressMap.REG_SCY:
            lcdRegs.set(LCDReg.SCY, data);
            break;
        case AddressMap.REG_SCX:
            lcdRegs.set(LCDReg.SCX, data);
            break;
        case AddressMap.REG_LY:
            lcdRegs.set(LCDReg.LY, data);
            break;
        case AddressMap.REG_LYC:
            lcdRegs.set(LCDReg.LYC, data);
            break;
        case AddressMap.REG_DMA:
            lcdRegs.set(LCDReg.DMA, data);
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

    private LcdImageLine computeLine(int lineIndex) {
        LcdImageLine nextLine = BLANK_LCD_IMAGE_LINE, bgSpriteLine = BLANK_LCD_IMAGE_LINE, fgSpriteLine = BLANK_LCD_IMAGE_LINE;
        
        int adjustedWX = lcdRegs.get(LCDReg.WX) - WX_OFFSET;
        
        int height = getHeight();

        // SPRITES
        List<LcdImageLine> composeSpriteLine = null;

        if (areSpritesActive()) {
            int[] spritesIndex = this.spritesIntersectingLine(lineIndex, getHeight());
            List<LcdImageLine> SpritesLine = this.listSpritesLine(spritesIndex);
            composeSpriteLine = composeSprites(spritesIndex, SpritesLine);
        }

        if (areSpritesActive()) {
            nextLine = composeSpriteLine.get(0);
        }
        
        if (isBackgroundActive()) {
            LcdImageLine nextBGLine = computeBGLine(lineIndex);
            nextLine = nextLine.below(nextBGLine, computeMeldOpacity(nextLine, nextBGLine));
        }

        if (lineIndex >= lcdRegs.get(LCDReg.WY) && isWindowActive(adjustedWX)) {                                               
            nextLine = nextLine.join(computeWinLine(winY, adjustedWX), adjustedWX);
            winY++;
        }

        if (areSpritesActive()) {
            nextLine = nextLine.below(composeSpriteLine.get(1));
        }
        
        return nextLine;
    }
    
    private LcdImageLine computeBGLine(int lineIndex) {
        LcdImageLine.Builder nextLineBuilderBG = new LcdImageLine.Builder(BG_SIZE);
        int scx = lcdRegs.get(LCDReg.SCX);
        int lineBG = Math.floorMod(lineIndex + lcdRegs.get(LCDReg.SCY), BG_SIZE);
        int tileLineBG = lineBG / TILE_LINE;
        int lineInTileBG = Math.floorMod(lineBG, TILE_LINE);
        int displayDataBG = (Bits.test(lcdRegs.get(LCDReg.LCDC), LCDC.BG_AREA))? 1 : 0;

        for (int i = 0; i < BG_TILE_SIZE; ++i) {
            int[] value = getMsbLsb(tileLineBG, lineInTileBG, displayDataBG, i);
            int msb = value[0];
            int lsb = value[1];
            nextLineBuilderBG.setBytes(8 * i, Bits.reverse8(msb), Bits.reverse8(lsb));
        }

        return nextLineBuilderBG.build().extractWrapped(scx, LCD_WIDTH).mapColors(lcdRegs.get(LCDReg.BGP));
    }
    
    private LcdImageLine computeWinLine(int lineIndex, int adjustedWX) {
        LcdImageLine.Builder nextLineBuilderWD = new LcdImageLine.Builder(WIN_SIZE);
        int tileLineWD = winY / TILE_LINE;
        int lineInTileWD = winY % TILE_LINE;
        int displayDataWD = (Bits.test(lcdRegs.get(LCDReg.LCDC), LCDC.WIN_AREA)) ? 1 : 0;
        adjustedWX = adjustedWX < 0 ? 0 : adjustedWX;
        
        for (int i = 0; i < BG_TILE_SIZE; ++i) {
            int[] value = getMsbLsb(tileLineWD, lineInTileWD, displayDataWD, i);
            int msb = value[0];
            int lsb = value[1];
            nextLineBuilderWD.setBytes(8 * i, Bits.reverse8(msb), Bits.reverse8(lsb));
        }
        
        return nextLineBuilderWD.build().mapColors(lcdRegs.get(LCDReg.BGP)).extractZeroExtended(-adjustedWX, LCD_WIDTH);
    }

    /**
     * Méthode permettant de calculer la valeur du msb et lsb d'un octet en
     * fonction de certains parametre
     * 
     * @param tileLine
     *            la ligne de la tuile
     * @param lineInTile
     *            la ligne dans la tuile
     * @param displayInfo
     *            l'information donnant le plage d'addresse ou récupérer les
     *            informations des tuiles
     * @param i
     *            l'index de l'octet qu'on veut sur la ligne
     * @return un tableau d'entier de taille 2 comportant les les 8bit du msb et
     *         les 8 bits du lsb
     */
    private int[] getMsbLsb(int tileLine, int lineInTile, int displayInfo, int i) {

        int lsb, msb;
        int tileNumber = this.read(AddressMap.BG_DISPLAY_DATA[displayInfo] + tileLine * BG_TILE_SIZE + i);
        if (lcdRegs.testBit(LCDReg.LCDC, LCDC.TILE_SOURCE)) {
            lsb = this.read(AddressMap.TILE_SOURCE[1] + tileNumber * TILE_SIZE + 2 * lineInTile);
            msb = this.read(AddressMap.TILE_SOURCE[1] + tileNumber * TILE_SIZE + 2 * lineInTile + 1);
        } else {
            tileNumber = tileNumber < TILE_SOURCE_NUMBER ? tileNumber + TILE_SOURCE_NUMBER : tileNumber - TILE_SOURCE_NUMBER;
            lsb = this.read(AddressMap.TILE_SOURCE[0] + tileNumber * TILE_SIZE + 2 * lineInTile);
            msb = this.read(AddressMap.TILE_SOURCE[0] + tileNumber * TILE_SIZE + 2 * lineInTile + 1);
        }

        return new int[] { msb, lsb };
    }

    private int[] spritesIntersectingLine(int ligne, int height) {
        int scanIndex = 0, foundSprites = 0;
        int[] spriteIntersect = new int[MAX_SPRITE];

        while (scanIndex < NUMBER_SPRITE && foundSprites < MAX_SPRITE) {
            int spriteY = oamRamController.readAttr(scanIndex, DISPLAY_DATA.Y_COORD) - 16;
            if (ligne >= spriteY && ligne < spriteY + height) {
                spriteIntersect[foundSprites] = Bits.make16(oamRamController.readAttr(scanIndex, DISPLAY_DATA.X_COORD), scanIndex);
                foundSprites += 1;
            }
            scanIndex++;
        }

        Arrays.sort(spriteIntersect, 0, foundSprites);

        int[] finalTab = new int[foundSprites];
        for (int i = 0; i < foundSprites; ++i) {
            finalTab[i] = Bits.clip(Byte.SIZE, spriteIntersect[i]);
        }

        return finalTab;
    }

    private int[] getSpriteOctet(int lineIndex, int index) {
        int valueOfIndex = oamRamController.readAttr(index, DISPLAY_DATA.TILE_INDEX);
        int spriteY = oamRamController.readAttr(index, DISPLAY_DATA.Y_COORD) - SPRITEY_OFF;
        int spriteSize = getHeight();
        boolean vFlip = oamRamController.readAttr(index, ATTRIBUTES.FLIP_V);

        int lineInTile = vFlip ? (spriteSize - 1 - Math.floorMod(lineIndex - spriteY, spriteSize)) : Math.floorMod(lineIndex - spriteY, spriteSize);
        int lsb = this.read(AddressMap.TILE_SOURCE[1] + TILE_SIZE * valueOfIndex + 2 * lineInTile);
        int msb = this.read(AddressMap.TILE_SOURCE[1] + TILE_SIZE * valueOfIndex + 2 * lineInTile + 1);
        return new int[] { msb, lsb };
    }

    /**
     * Méthode qui calcule la ligne de chaque sprite afin de pouvoir les
     * composer ensemble par la suite
     * 
     * @param spritesIndex
     *            le tableau contenant les indexs des sprites intersectant la
     *            ligne en cours de dessin
     * @return une liste de ligne correspondant aux lignes de chaque sprite
     */
    private List<LcdImageLine> listSpritesLine(int[] spritesIndex) {
        List<LcdImageLine> listSpritesLine = new ArrayList<>();

        for (int i = 0; i < spritesIndex.length; ++i) {
            boolean isFlipH = oamRamController.readAttr(spritesIndex[i], ATTRIBUTES.FLIP_H);
            int[] msbLsb = getSpriteOctet(lcdRegs.get(LCDReg.LY), spritesIndex[i]);
            int spriteX = oamRamController.readAttr(spritesIndex[i], DISPLAY_DATA.X_COORD) - SPRITE_XOFFSET;
            boolean palette = oamRamController.readAttr(spritesIndex[i], ATTRIBUTES.PALETTE);

            LcdImageLine.Builder lineBuilder = new LcdImageLine.Builder(LCD_WIDTH);
            
            int msb = msbLsb[0];
            int lsb = msbLsb[1];
            if (!isFlipH) {
                 lineBuilder.setBytes(0, Bits.reverse8(msb), Bits.reverse8(lsb));
            } else {
                lineBuilder.setBytes(0, msb, lsb);
            }

            LcdImageLine line = lineBuilder.build().mapColors(palette ? lcdRegs.get(LCDReg.OBP1) : lcdRegs.get(LCDReg.OBP0)).shift(-spriteX);

            listSpritesLine.add(line);
        }
        return listSpritesLine;
    }

    /**
     * Méthode qui compose les lignes de sprite ensembl. 2 lignes sont créees, 1
     * pour les sprites d'arrière plan, 1 pour ceux de premier plan
     * 
     * @param index
     *            le tableau contenant les index des spirtes intersectant la
     *            ligne en cours de dessin
     * @param spriteLine
     *            le tableau comportant les lignes de chaque sprites
     * @return
     */
    private List<LcdImageLine> composeSprites(int[] index, List<LcdImageLine> spriteLine) {

        LcdImageLine behind = EMPTY_LINE;
        LcdImageLine inFront = EMPTY_LINE;

        for (int i = index.length - 1; i >= 0; --i) {
            boolean isBehind = oamRamController.readAttr(index[i], ATTRIBUTES.BEHIND_BG);
            if (isBehind) {
                behind = behind.below(spriteLine.get(i));
            } else {
                inFront = inFront.below(spriteLine.get(i));
            }
        }
        
        List<LcdImageLine> list = new ArrayList<>();
        list.add(behind);
        list.add(inFront);
        return list;
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
        return TILE_LINE * (lcdRegs.testBit(LCDReg.LCDC, LCDC.OBJ_SIZE) ? 2 : 1);
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
        Objects.requireNonNull(bus);
        this.bus = bus;
        bus.attach(this);
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
        
        if (address == AddressMap.REG_DMA) System.out.println("activate dma");

        if (address >= AddressMap.VRAM_START
                && address < AddressMap.VRAM_END) {
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
            case AddressMap.REG_SCY:
                lcdRegs.set(LCDReg.SCY, data);
                break;
            case AddressMap.REG_SCX:
                lcdRegs.set(LCDReg.SCX, data);
                break;
            case AddressMap.REG_LY:
                
                break;
            case AddressMap.REG_LYC:
                modifyLYorLYC(LCDReg.LYC, data);
                break;
            case AddressMap.REG_DMA:
                // Active la copie directe dans la mémoire OAM (spriteRam)
                if (!copyActive) {
                    this.copyActive = true;
                    this.counterOfCopy = 0;
                }
            default:
                setRegValue(address, data);
                break;
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