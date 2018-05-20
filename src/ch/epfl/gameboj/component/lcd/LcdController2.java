package ch.epfl.gameboj.component.lcd;

import static ch.epfl.gameboj.component.lcd.LcdImage.BLANK_LCD_IMAGE;

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
import ch.epfl.gameboj.component.memory.Ram;
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

    private final Ram ramVideo;
    private final Ram ramSprite;
    
    /**
     * Enumération représentant l'octet de l'entier (type int) contenant les
     * information dites (Y, X, INDEX et INFO) d'un sprite
     * 
     *
     */
    private enum SPRITE implements Bit {
        Y, X, INDEX, INFO
    }

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
        Objects.requireNonNull(cpu);
        this.cpu = cpu;
        ramVideo = new Ram(AddressMap.VRAM_SIZE);
        ramSprite = new Ram(AddressMap.OAM_SIZE);

    }

    @Override
    public void cycle(long cycle) {

        if (nextNonIdleCycle == Long.MAX_VALUE && isOn()) {
            turnOn(cycle);
        }

        if (this.copyActive && this.counterOfCopy < 160) {
            this.write(AddressMap.OAM_START + this.counterOfCopy, bus
                    .read((lcdRegs.get(LCDReg.DMA) << 8) + this.counterOfCopy));
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
                this.nextImage = this.nextImageBuilder.build();
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
     * Methode qui retourne l'image actuelle qui vient d'être construite
     * 
     * @return nextImage, l'image construite à l'instant t ou on appel cette
     *         fonction
     */
    public LcdImage currentImage() {
        return this.nextImage == null ? EMPTY_IMAGE : nextImage;
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
        case 0: {
            if (lcdRegs.testBit(LCDReg.STAT, STAT.INT_MODE0))
                cpu.requestInterrupt(Interrupt.LCD_STAT);
        }
            break;
        case 1: {
            if (lcdRegs.testBit(LCDReg.STAT, STAT.INT_MODE1))
                cpu.requestInterrupt(Interrupt.LCD_STAT);
            cpu.requestInterrupt(Interrupt.VBLANK);
        }
            break;
        case 2: {
            if (lcdRegs.testBit(LCDReg.STAT, STAT.INT_MODE2))
                cpu.requestInterrupt(Interrupt.LCD_STAT);
        }
            break;
        case 3: {
        }
            break;

        }
    }

    private int getRegValue(int address) {
        Preconditions.checkBits16(address);
        int value = 0;
        switch (address) {
        case AddressMap.REG_LCDC:
            value = lcdRegs.get(LCDReg.LCDC);
            break;
        case AddressMap.REG_LCD_STAT:
            value = lcdRegs.get(LCDReg.STAT);
            break;
        case AddressMap.REG_SCY:
            value = lcdRegs.get(LCDReg.SCY);
            break;
        case AddressMap.REG_SCX:
            value = lcdRegs.get(LCDReg.SCX);
            break;
        case AddressMap.REG_LY:
            value = lcdRegs.get(LCDReg.LY);
            break;
        case AddressMap.REG_LYC:
            value = lcdRegs.get(LCDReg.LYC);
            break;
        case AddressMap.REG_DMA:
            value = lcdRegs.get(LCDReg.DMA);
            break;
        case AddressMap.REG_BGP:
            value = lcdRegs.get(LCDReg.BGP);
            break;
        case AddressMap.REG_OBP0:
            value = lcdRegs.get(LCDReg.OBP0);
            break;
        case AddressMap.REG_OBP1:
            value = lcdRegs.get(LCDReg.OBP1);
            break;
        case AddressMap.REG_WY:
            value = lcdRegs.get(LCDReg.WY);
            break;
        case AddressMap.REG_WX:
            value = lcdRegs.get(LCDReg.WX);
            break;
        }

        return value;
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

    private LcdImageLine computeLine(int ligne) {

        // BACKGROUND
        LcdImageLine.Builder nextLineBuilderBG = new LcdImageLine.Builder(
                BG_SIZE);
        int scx = lcdRegs.get(LCDReg.SCX);
        int lineBG = Math.floorMod(ligne + lcdRegs.get(LCDReg.SCY), BG_SIZE);
        int tileLineBG = lineBG / TILE_LINE;
        int lineInTileBG = Math.floorMod(lineBG, TILE_LINE);
        int displayDataBG = (Bits.test(lcdRegs.get(LCDReg.LCDC), LCDC.BG_AREA))
                ? 1
                : 0;

        // FENETRE
        LcdImageLine.Builder nextLineBuilderWD = new LcdImageLine.Builder(
                BG_SIZE);
        int tileLineWD = winY / TILE_LINE;
        int lineInTileWD = winY % TILE_LINE;
        int displayDataWD = (Bits.test(lcdRegs.get(LCDReg.LCDC), LCDC.WIN_AREA))
                ? 1
                : 0;
        int wxPrime = lcdRegs.get(LCDReg.WX) - WX_OFF;
        wxPrime = wxPrime < 0 ? 0 : wxPrime; // POUR REGLER ZELDA, VU AVEC LE
                                             // PROF
        int wy = lcdRegs.get(LCDReg.WY);

        // SPRITES
        List<LcdImageLine> composeSpriteLine = null;
        BitVector opacityToBelow = EMPTY_VECTOR;

        if (Bits.test(lcdRegs.get(LCDReg.LCDC), LCDC.OBJ)) {
            int[] spritesIndex = this.spritesIntersectingLine(ligne);
            List<LcdImageLine> SpritesLine = this.listSpritesLine(spritesIndex);
            composeSpriteLine = composeSprites(spritesIndex, SpritesLine);
        }

        // COMMUN
        int msb;
        int lsb;
        int palette = lcdRegs.get(LCDReg.BGP);

        // CALCUL DU BACKGROUND
        if (lcdRegs.testBit(LCDReg.LCDC, LCDC.BG)) {
            for (int i = 0; i < BG_TILE_SIZE; ++i) {
                int[] value = getMsbLsb(tileLineBG, lineInTileBG, displayDataBG,
                        i);
                msb = value[0];
                lsb = value[1];
                nextLineBuilderBG.setBytes(8 * i, Bits.reverse8(msb),
                        Bits.reverse8(lsb));
            }
        }

        LcdImageLine nextImageLine = nextLineBuilderBG.build()
                .mapColors(palette).extractWrapped(scx, LCD_WIDTH);

        // CALCUL L'OPACITE ENTRE LE BACKGROUND ET LES SPRITES D'ARRIERE PLAN ET
        // COMPOSE LES DEUX LIGNES ENSMEMBLES
        if (lcdRegs.testBit(LCDReg.LCDC, LCDC.OBJ)) {
            opacityToBelow = nextImageLine.getOpacity().not()
                    .and(composeSpriteLine.get(0).getOpacity());

            nextImageLine = nextImageLine.below(composeSpriteLine.get(0),
                    opacityToBelow);
        }

        // DESSIN DE LA FENETRE
        if (lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN) && wxPrime >= 0
                && wxPrime < LCD_WIDTH && wy <= ligne) {
            for (int i = 0; i < BG_TILE_SIZE; ++i) {
                int[] value = getMsbLsb(tileLineWD, lineInTileWD, displayDataWD,
                        i);
                msb = value[0];
                lsb = value[1];
                nextLineBuilderWD.setBytes(8 * i, Bits.reverse8(msb),
                        Bits.reverse8(lsb));
            }
            winY += 1;

//            nextImageLine = nextImageLine.join(
//                    nextLineBuilderWD.build().mapColors(palette).shift(wxPrime)
//                            .extractWrapped(0, LCD_WIDTH),
//                    wxPrime);
            
            LcdImageLine windowLine = nextLineBuilderWD.build().mapColors(palette).extractWrapped(-wxPrime, LCD_WIDTH);
            nextImageLine = nextImageLine.join(windowLine, wxPrime);

        }

        // DESSIN DES SPRITE DU PREMIER PLAN
        if (lcdRegs.testBit(LCDReg.LCDC, LCDC.OBJ)) {
            nextImageLine = nextImageLine.below(composeSpriteLine.get(1));
        }

        // RETOURNE LA LIGNE COMPOSEE DES DIFFERENTES LIGNE (FENETRE +
        // BACKGROUND + SPRITES)
        return nextImageLine;

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
    private int[] getMsbLsb(int tileLine, int lineInTile, int displayInfo,
            int i) {

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

    /**
     * Méthode permttant de calculer les sprites intersectant la ligne en cours
     * de dessin en fonction des règles de priorité (coord X puis index)
     * 
     * @param ligne
     *            la ligne en cours de dessin
     * @return un tableau d'entier contenant les indexs des sprites intersecant
     *         la ligne
     */
    private int[] spritesIntersectingLine(int ligne) {

        int index = 0;
        int spriteFound = 0;
        int[] spriteIntersect = new int[MAX_SPRITE];

        while (index < NUMBER_SPRITE && spriteFound < MAX_SPRITE) {
            int spriteSize = getHeight();
            int spriteY = this.getSpriteInfo(index, SPRITE.Y) - SPRITEY_OFF;
            if (ligne >= spriteY && ligne < spriteY + spriteSize) {
                // on enlève pas SPRITEX_OFF pour ne pas avoir de coordonnées
                // négatives qui fausseraient le calcul de priortié des sprites
                int spriteX = this.getSpriteInfo(index, SPRITE.X);
                spriteIntersect[spriteFound] = Bits.make16(spriteX, index);
                spriteFound += 1;
            }
            index += 1;
        }

        Arrays.sort(spriteIntersect, 0, spriteFound);

        int[] finalTab = new int[spriteFound];
        for (int i = 0; i < spriteFound; ++i) {
            finalTab[i] = Bits.clip(Byte.SIZE, spriteIntersect[i]);
        }

        return finalTab;

    }

    /**
     * Méthode similaire a getMsbLsb mais adapté au sprite (leur taille, flipH
     * et flipV)
     * 
     * @param index
     *            l'index du sprite dont on veut calculer les valeurs msb et lsb
     * @return un tableau d'int contenant les valeurs msb et lsb de l'octet du
     *         sprite
     */
    private int[] getSpriteOctet(int index) {

        int lineIndex = lcdRegs.get(LCDReg.LY);
        int valueOfIndex = this.getSpriteInfo(index, SPRITE.INDEX);
        int spriteY = this.getSpriteInfo(index, SPRITE.Y) - SPRITEY_OFF;
        int spriteSize = getHeight();
        boolean isFlipV = Bits.test(this.getSpriteInfo(index, SPRITE.INFO),
                SINFO.FLIP_V);

        int lineInTile = isFlipV
                ? (spriteSize - 1
                        - Math.floorMod(lineIndex - spriteY, spriteSize))
                : Math.floorMod(lineIndex - spriteY, spriteSize);
        int lsb = this.read(AddressMap.TILE_SOURCE[1] + TILE_SIZE * valueOfIndex
                + 2 * lineInTile);
        int msb = this.read(AddressMap.TILE_SOURCE[1] + TILE_SIZE * valueOfIndex
                + 2 * lineInTile + 1);
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
            boolean isFlipH = Bits.test(
                    this.getSpriteInfo(spritesIndex[i], SPRITE.INFO),
                    SINFO.FLIP_H);
            int[] msbLsb = getSpriteOctet(spritesIndex[i]);
            int spriteX = this.getSpriteInfo(spritesIndex[i], SPRITE.X)
                    - SPRITEX_OFF;
            int palette = Bits.test(
                    this.getSpriteInfo(spritesIndex[i], SPRITE.INFO),
                    SINFO.PALETTE) ? lcdRegs.get(LCDReg.OBP1)
                            : lcdRegs.get(LCDReg.OBP0);

            int msb = msbLsb[0];
            int lsb = msbLsb[1];
            if (!isFlipH) {
                msb = Bits.reverse8(msb);
                lsb = Bits.reverse8(lsb);
            }

            LcdImageLine.Builder lineBuilder = new LcdImageLine.Builder(
                    LCD_WIDTH).setBytes(8, msb, lsb);
            LcdImageLine line = lineBuilder.build().mapColors(palette)
                    .shift(spriteX - 64);

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
            boolean isBehind = Bits.test(this.getSpriteInfo(index[i], SPRITE.INFO), SINFO.BEHIND_BG);
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

    /**
     * Méthode permettant de recuprer les différents informations d'un sprite
     * 
     * @param spriteIndex
     *            l'index du spirte dont on veut récuperer l'info
     * @param info
     *            le type d'info (Y, X, INDEX, INFO) SPRITE
     * @return l'info voulu
     */
    private int getSpriteInfo(int spriteIndex, SPRITE info) {
        return this.read(AddressMap.OAM_START + spriteIndex * SPRITE_INFO + info.index());
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
        return TILE_LINE * (lcdRegs.testBit(LCDReg.LCDC, LCDC.OBJ_SIZE) ? 2 : 1);
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
        return (adjustedWX >= 0 && adjustedWX < 160 && lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN));
    }
    
    private boolean isBackgroundActive() {
        return lcdRegs.testBit(LCDReg.LCDC, LCDC.BG);
    }

    @Override
    public void attachTo(Bus bus) {
        Objects.requireNonNull(bus);
        this.bus = bus;
        bus.attach(this);
    }
    
    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);
        if (address >= AddressMap.VRAM_START && address < AddressMap.VRAM_END) {
            return this.ramVideo.read(address - AddressMap.VRAM_START);
        } else {
            if (address >= AddressMap.REGS_LCD_START && address < AddressMap.REGS_LCD_END) {
                int data = getRegValue(address);
                return data;
            } else {
                if (address >= AddressMap.OAM_START && address < AddressMap.OAM_END) {
                    return this.ramSprite.read(address - AddressMap.OAM_START);
                } else {
                    return NO_DATA;
                }
            }
        }
    }

    @Override
    public void write(int address, int data) {
        Preconditions.checkBits8(data);
        
        if (address == AddressMap.REG_DMA) System.out.println("activate dma");

        if (address >= AddressMap.VRAM_START
                && address < AddressMap.VRAM_END) {
            this.ramVideo.write(address - AddressMap.VRAM_START, data);
        }

        if (address >= AddressMap.REGS_START
                && address < AddressMap.REGS_END) {

            switch (address) {
            case AddressMap.REG_LCDC: {
                lcdRegs.set(LCDReg.LCDC, data);
                if (!(Bits.test(data, 7))) {
                    turnOff();
                }
            }
                break;

            case AddressMap.REG_LCD_STAT: {
                int newStat = (data & 0b11111000) | ((lcdRegs.get(LCDReg.STAT) & 0b00000111));
                lcdRegs.set(LCDReg.STAT, newStat);
            }
                break;

            case AddressMap.REG_LY: {
                // DO NOTHING
                // cf. 1.2.1 etape 9
                // ici car il ne faut pas rentrer dans le case défault
            }
                break;

            case AddressMap.REG_LYC: {
                this.modifyLYorLYC(LCDReg.LYC, data);
            }
                break;
            case AddressMap.REG_DMA: {
                // Active la copie directe dans la mémoire OAM (spriteRam)
                if (!copyActive) {
                    this.copyActive = true;
                    this.counterOfCopy = 0;
                }
            }

            default: {
                setRegValue(address, data);
            }
                break;
            }
        }

        if (address >= AddressMap.OAM_START && address < AddressMap.OAM_END) {
            this.ramSprite.write(address - AddressMap.OAM_START, data);
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