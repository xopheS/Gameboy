package ch.epfl.gameboj.component.lcd;

import java.util.ArrayList;
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
import ch.epfl.gameboj.component.memory.Ram;

/**
 * 
 * 
 * Classe qui représente l'écran de la GameBoy
 * 
 * @author Auguste Lefevre (269821)
 * @author Marc Watine (269508)
 */
public final class LcdController implements Component, Clocked {

    public static final int LCD_WIDTH = 160;
    public static final int LCD_HEIGHT = 144;

    private static final int BG_SIZE = 256;
    private static final int TILE_LINE = 8;
    private static final int TILE_SIZE = 16;
    private static final int TILE_BY_BG = 32;
    private static final int IMAGE_DRAW = 17556;
    private static final int LINE_DRAW_CYCLE = 114;
    private static final int END_IMAGE_DRAW = 16416;
    private static final int TILE_SOURCE_NUMBER = 128;
    private static final int WX_OFF = 7;
    private static final int SPRITE_INFO = 4;
    private static final int SPRITEY_OFF = 16;
    private static final int SPRITEX_OFF = 8;
    private static final int MODE2_DURATION = 20;
    private static final int MODE3_DURATION = 43;
    private static final int MODE0_DURATION = 51;
    private static final int MAX_SPRITE = 10;
    private static final int NUMBER_SPRITE = 40;
    private static final BitVector EMPTY_VECTOR = new BitVector(LCD_WIDTH,
            false);
    private static final LcdImageLine EMPTY_LINE = new LcdImageLine(
            EMPTY_VECTOR, EMPTY_VECTOR, EMPTY_VECTOR);
    private static final LcdImage EMPTY_IMAGE = new LcdImage(LCD_WIDTH,
            LCD_HEIGHT, new ArrayList<LcdImageLine>(
                    Collections.nCopies(LCD_HEIGHT, EMPTY_LINE)));

    private final Cpu cpu;
    private final Ram ramVideo;
    private final Ram ramSprite;
    private final RegisterFile<Reg> register = new RegisterFile<>(Reg.values());

    private Bus bus;
    private LcdImage.Builder nextImageBuilder;
    private LcdImage nextImage;
    private int winY = 0;
    private int indexLine = 0;
    private boolean copyActive = false;
    private int counterOfCopy = 0;
    private long nextNonIdleCycle = Long.MAX_VALUE;
    private long lcdOnCycle = Long.MAX_VALUE;
    
    /**
     * Enumération représentant les différents registre de l'écran (LCD)
     *
     */
    private enum Reg implements Register {
        LCDC, STAT, SCY, SCX, LY, LYC, DMA, BGP, OBP0, OBP1, WY, WX
    }

    /**
     * Enumération représentant les différents bit du registre LCDC
     *
     */
    private enum LCDC implements Bit {
        BG, OBJ, OBJ_SIZE, BG_AREA, TILE_SOURCE, WIN, WIN_AREA, LCD_STATUS
    }

    /**
     * Enumération représentant les différents bit du registre STAT
     *
     */
    private enum STAT implements Bit {
        MODE0, MODE1, LYC_EQ_LY, INT_MODE0, INT_MODE1, INT_MODE2, INT_LYC, UNUSED7
    }

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
    public LcdController(Cpu cpu) {
        Objects.requireNonNull(cpu);
        this.cpu = cpu;
        ramVideo = new Ram(AddressMap.VRAM_SIZE);
        ramSprite = new Ram(AddressMap.OAM_SIZE);
    }
    
    /**
     * Redéfinition de la méthode attachTo afin de pouvoir ajouter le bus comme
     * attribut dans cette classe
     */
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
            if (address >= AddressMap.REGS_LCD_START && address < AddressMap.REGS_END) {
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
    private void changeMode(int value) {
        Preconditions.checkArgument(value >= 0 && value <= 3);
        switch (value) {
        case 0: {
            register.setBit(Reg.STAT, STAT.MODE0, false);
            register.setBit(Reg.STAT, STAT.MODE1, false);
            if (Bits.test(register.get(Reg.STAT), STAT.INT_MODE0))
                cpu.requestInterrupt(Interrupt.LCD_STAT);
        }
            break;
        case 1: {
            register.setBit(Reg.STAT, STAT.MODE0, true);
            register.setBit(Reg.STAT, STAT.MODE1, false);
            if (Bits.test(register.get(Reg.STAT), STAT.INT_MODE1))
                cpu.requestInterrupt(Interrupt.LCD_STAT);
            cpu.requestInterrupt(Interrupt.VBLANK);
        }
            break;
        case 2: {
            register.setBit(Reg.STAT, STAT.MODE0, false);
            register.setBit(Reg.STAT, STAT.MODE1, true);
            if (Bits.test(register.get(Reg.STAT), STAT.INT_MODE2))
                cpu.requestInterrupt(Interrupt.LCD_STAT);
        }
            break;
        case 3: {
            register.setBit(Reg.STAT, STAT.MODE0, true);
            register.setBit(Reg.STAT, STAT.MODE1, true);
        }
            break;

        }
    }

    @Override
    public void cycle(long cycle) {
     // Allumage de l'écran
        if (this.nextNonIdleCycle == Long.MAX_VALUE
                && Bits.test(register.get(Reg.LCDC), LCDC.LCD_STATUS)) {
            this.lcdOnCycle = cycle;
            this.nextNonIdleCycle = 0;
        }

        // Copie direct de la spriteRam
        if (this.copyActive && this.counterOfCopy < 160) {
            this.write(AddressMap.OAM_START + this.counterOfCopy, bus.read((register.get(Reg.DMA) << 8) + this.counterOfCopy));
            this.counterOfCopy += 1;
            if (this.counterOfCopy == 160) {
                this.copyActive = false;
                this.counterOfCopy = 0;
            }
        }

        // Calcul si il y a quelque chose à faire à ce cycle, si oui appelle la
        // méthode reallyCycle
        
        if ((cycle - this.lcdOnCycle) == this.nextNonIdleCycle) {
            indexLine = ((((int) ((cycle - this.lcdOnCycle))) % IMAGE_DRAW)
                    / LINE_DRAW_CYCLE);
            this.reallyCycle(indexLine, (int) (((cycle - this.lcdOnCycle) % IMAGE_DRAW) % LINE_DRAW_CYCLE));
        }
//        
//        if (nextNonIdleCycle == Long.MAX_VALUE && isOn()) {
//            turnOn(cycle);
//        }
//
//        if (dmaController.isActive()) {
//            dmaController.copy();
//        }
//
//        cyc = cycle;
//
//        if (isOn()) {
//            long cycFromPowOn = cycle - lcdOnCycle; 
//            cycFromImg = (int) (cycFromPowOn % IMAGE_CYCLE_DURATION);
//            currentImage = (int) Math.floorDiv(cycFromPowOn, IMAGE_CYCLE_DURATION);
//
//            if (cycle == nextNonIdleCycle) {
//                int currentLine = (int) (cycFromImg / LINE_CYCLE_DURATION);
//                cycFromLn = cycFromImg % LINE_CYCLE_DURATION;
//                reallyCycle(cycle, currentLine, (int) cycFromLn);
//            }
//        }
    }

    private void reallyCycle(int currentLine, int cycFromLn) {       
     // Cas ou les 144 lignes sont déssinés mais qu'il reste l'équivalent de
        // 10 lignes (en cycle soit 114*10) avant de passer au dessin de la
        // prochaine image
        if (this.nextNonIdleCycle % (IMAGE_DRAW) >= END_IMAGE_DRAW) {
            if (this.nextNonIdleCycle % (IMAGE_DRAW) == END_IMAGE_DRAW) {
                this.changeMode(1);
                this.nextImage = this.nextImageBuilder.build();
                this.winY = 0;
            }

            this.nextNonIdleCycle += LINE_DRAW_CYCLE;
            this.modifyLYorLYC(Reg.LY, this.indexLine);

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
            this.modifyLYorLYC(Reg.LY, this.indexLine);
            this.changeMode(2);
            break;

        case MODE2_DURATION:
            this.nextNonIdleCycle += MODE3_DURATION;
            // System.out.println(register.get(Reg.LY));
            this.nextImageBuilder.setLine(register.get(Reg.LY),
                    this.computeLine(register.get(Reg.LY)));
            this.changeMode(3);
            break;

        case MODE2_DURATION + MODE3_DURATION:
            this.nextNonIdleCycle += MODE0_DURATION;
            this.changeMode(0);
            break;

        }
        
        
//        modifyLYorLYC(Reg.LY, currentLine);
//        
//        if (currentLine < 144) {
//            switch (cycFromLn) {
//            case MODE2_DURATION:
//                nextNonIdleCycle += MODE3_DURATION;
//                setMode(3);
//                break;
//            case MODE2_DURATION + MODE3_DURATION:
//                nextNonIdleCycle += MODE0_DURATION;
//                setMode(0);
//                nextImageBuilder.setLine(currentLine, computeLine(currentLine));
//                break;
//            case 0:
//                nextNonIdleCycle += MODE2_DURATION;
//                setMode(2);
//                break;
//            }
//        } else {
//            nextNonIdleCycle += LINE_CYCLE_DURATION;
//            if (currentLine == 144) {
//                setMode(1);
//                displayedImage = nextImageBuilder.build();
//                nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
//            } else if (currentLine == 153) {
//                winY = 0;
//            }
//        }
    }

    private void turnOff() {
        // TODO power off only possible during VBLANK
        changeMode(0);
        modifyLYorLYC(Reg.LY, 0);
        nextNonIdleCycle = Long.MAX_VALUE;
        System.out.println("turnoff");
    }

    private void turnOn(long cycle) {
        lcdOnCycle = cycle;
        nextNonIdleCycle = cycle;
        changeMode(2);
    }

    private LcdImageLine computeLine(int lineIndex) {
        
     // BACKGROUND
        LcdImageLine.Builder nextLineBuilderBG = new LcdImageLine.Builder(
                BG_SIZE);
        int scx = register.get(Reg.SCX);
        int lineBG = Math.floorMod(lineIndex + register.get(Reg.SCY), BG_SIZE);
        int tileLineBG = lineBG / TILE_SIZE;
        int lineInTileBG = Math.floorMod(lineBG, TILE_SIZE);
        int displayDataBG = (Bits.test(register.get(Reg.LCDC), LCDC.BG_AREA))
                ? 1
                : 0;

        // FENETRE
        LcdImageLine.Builder nextLineBuilderWD = new LcdImageLine.Builder(
                BG_SIZE);
        int tileLineWD = winY / TILE_SIZE;
        int lineInTileWD = winY % TILE_SIZE;
        int displayDataWD = (Bits.test(register.get(Reg.LCDC), LCDC.WIN_AREA))
                ? 1
                : 0;
        int wxPrime = register.get(Reg.WX) - WX_OFF;
        wxPrime = wxPrime < 0 ? 0 : wxPrime; // POUR REGLER ZELDA, VU AVEC LE
                                             // PROF
        int wy = register.get(Reg.WY);

        // SPRITES
        List<LcdImageLine> composeSpriteLine = null;
        BitVector opacityToBelow = EMPTY_VECTOR;

        if (Bits.test(register.get(Reg.LCDC), LCDC.OBJ)) {
            int[] spritesIndex = this.spritesIntersectingLine(lineIndex);
            List<LcdImageLine> SpritesLine = this.listSpritesLine(spritesIndex);
            composeSpriteLine = composeSprites(spritesIndex, SpritesLine);
        }

        // COMMUN
        int msb;
        int lsb;
        int palette = register.get(Reg.BGP);

        // CALCUL DU BACKGROUND
        if (register.testBit(Reg.LCDC, LCDC.BG)) {
            for (int i = 0; i < TILE_BY_BG; ++i) {
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
        if (register.testBit(Reg.LCDC, LCDC.OBJ)) {
            opacityToBelow = nextImageLine.getOpacity().not().and(composeSpriteLine.get(0).getOpacity());

            nextImageLine = nextImageLine.below(composeSpriteLine.get(0),
                    opacityToBelow);
        }

        // DESSIN DE LA FENETRE
        if (register.testBit(Reg.LCDC, LCDC.WIN) && wxPrime >= 0
                && wxPrime < LCD_WIDTH && wy <= lineIndex) {
            for (int i = 0; i < TILE_BY_BG; ++i) {
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
        if (register.testBit(Reg.LCDC, LCDC.OBJ)) {
            nextImageLine = nextImageLine.below(composeSpriteLine.get(1));
        }

        // RETOURNE LA LIGNE COMPOSEE DES DIFFERENTES LIGNE (FENETRE +
        // BACKGROUND + SPRITES)
        return nextImageLine;
        
        
//        LcdImageLine nextLine = BLANK_LCD_IMAGE_LINE, bgSpriteLine = BLANK_LCD_IMAGE_LINE,
//                fgSpriteLine = BLANK_LCD_IMAGE_LINE;
//
//        int adjustedWX = register.get(Reg.WX) - WX_OFFSET;
//        
//        int height = getHeight();
//
//        if (register.testBit(Reg.LCDC, LCDC.OBJ)) {
//            List<Integer> bgSpriteInfoL = new ArrayList<>(10);
//            List<Integer> fgSpriteInfoL = new ArrayList<>(10);
//            Integer[] spriteInfo = oamRamController.spritesIntersectingLine(lineIndex, height);
//
//            for (int i = 0; i < spriteInfo.length; ++i) {
//                boolean isInBG = oamRamController.readAttr(i, ATTRIBUTES.BEHIND_BG);
//                if (isInBG) {
//                    bgSpriteInfoL.add(spriteInfo[i]);
//                } else {
//                    fgSpriteInfoL.add(spriteInfo[i]);
//                }
//            }
//
//            Integer[] bgSpriteInfo = bgSpriteInfoL.toArray(new Integer[0]);
//            Integer[] fgSpriteInfo = fgSpriteInfoL.toArray(new Integer[0]);
//
//            bgSpriteLine = computeSpriteLine(bgSpriteInfo, lineIndex);
//            fgSpriteLine = computeSpriteLine(fgSpriteInfo, lineIndex);
//        }
//
//        nextLine = bgSpriteLine;
//
//        if (isBackgroundActive()) {
//            LcdImageLine nextBGLine = computeBGLine(lineIndex).extractWrapped(register.get(Reg.SCX), LCD_WIDTH);
//            nextLine = nextLine.below(nextBGLine, computeMeldOpacity(nextLine, nextBGLine));
//        }
//
//        if (lineIndex >= register.get(Reg.WY) && isWindowActive(adjustedWX)) {
//            LcdImageLine winLine = computeWinLine(adjustedWX, winY++, LCD_WIDTH);
//            nextLine = nextLine.join(winLine, adjustedWX);
//        }
//
//        nextLine = nextLine.below(fgSpriteLine);
//
//        return nextLine;
    }
    
    private List<LcdImageLine> listSpritesLine(int[] spritesIndex) {
//        List<LcdImageLine> listSpritesLine = new ArrayList<>();
//
//        for (int i = 0; i < spritesIndex.length; ++i) {
//            boolean isFlipH = Bits.test(
//                    this.getSpriteInfo(spritesIndex[i], SPRITE.INFO),
//                    SINFO.FLIP_H);
//            int[] msbLsb = getSpriteOctet(spritesIndex[i]);
//            int spriteX = this.getSpriteInfo(spritesIndex[i], SPRITE.X)
//                    - SPRITEX_OFF;
//            int palette = Bits.test(
//                    this.getSpriteInfo(spritesIndex[i], SPRITE.INFO),
//                    SINFO.PALETTE) ? register.get(Reg.OBP1)
//                            : register.get(Reg.OBP0);
//
//            int msb = msbLsb[0];
//            int lsb = msbLsb[1];
//            if (!isFlipH) {
//                msb = Bits.reverse8(msb);
//                lsb = Bits.reverse8(lsb);
//            }
//
//            LcdImageLine.Builder lineBuilder = new LcdImageLine.Builder(
//                    LCD_WIDTH).setBytes(8, msb, lsb);
//            LcdImageLine line = lineBuilder.build().mapColors(palette)
//                    .shift(spriteX - 64);
//
//            listSpritesLine.add(line);
//        }
//        return listSpritesLine;
        
        return new ArrayList<>();
    }
    
    private int[] spritesIntersectingLine(int ligne) {

//        int index = 0;
//        int spriteFound = 0;
//        int[] spriteIntersect = new int[MAX_SPRITE];
//
//        while (index < NUMBER_SPRITE && spriteFound < MAX_SPRITE) {
//            int spriteSize = getSize();
//            int spriteY = this.getSpriteInfo(index, SPRITE.Y) - SPRITEY_OFF;
//            if (ligne >= spriteY && ligne < spriteY + spriteSize) {
//                // on enlève pas SPRITEX_OFF pour ne pas avoir de coordonnées
//                // négatives qui fausseraient le calcul de priortié des sprites
//                int spriteX = this.getSpriteInfo(index, SPRITE.X);
//                spriteIntersect[spriteFound] = Bits.make16(spriteX, index);
//                spriteFound += 1;
//            }
//            index += 1;
//        }
//
//        Arrays.sort(spriteIntersect, 0, spriteFound);
//
//        int[] finalTab = new int[spriteFound];
//        for (int i = 0; i < spriteFound; ++i) {
//            finalTab[i] = Bits.clip(Byte.SIZE, spriteIntersect[i]);
//        }
//
//        return finalTab;
        
        return new int[] {0, 0, 0};

    }
    
    private int[] getMsbLsb(int tileLine, int lineInTile, int displayInfo, int i) {

//        int lsb, msb;
//        int tileNumber = this.read(AddressMap.BG_DISPLAY_DATA[displayInfo]
//                + tileLine * TILE_BY_BG + i);
//        if (register.testBit(Reg.LCDC, LCDC.TILE_SOURCE)) {
//            lsb = this.read(AddressMap.TILE_SOURCE[1] + tileNumber * TILE_SIZE
//                    + 2 * lineInTile);
//            msb = this.read(AddressMap.TILE_SOURCE[1] + tileNumber * TILE_SIZE
//                    + 2 * lineInTile + 1);
//        } else {
//            tileNumber = tileNumber < TILE_SOURCE_NUMBER
//                    ? tileNumber + TILE_SOURCE_NUMBER
//                    : tileNumber - TILE_SOURCE_NUMBER;
//            lsb = this.read(AddressMap.TILE_SOURCE[0] + tileNumber * TILE_SIZE
//                    + 2 * lineInTile);
//            msb = this.read(AddressMap.TILE_SOURCE[0] + tileNumber * TILE_SIZE
//                    + 2 * lineInTile + 1);
//        }
//
//        return new int[] { msb, lsb };
        
        return new int[] {0, 0};
    }
    
    private List<LcdImageLine> composeSprites(int[] index, List<LcdImageLine> spriteLine) {

        LcdImageLine behind = EMPTY_LINE;
        LcdImageLine inFront = EMPTY_LINE;
//
//        for (int i = index.length - 1; i >= 0; --i) {
//            boolean isBehind = Bits.test(
//                    this.getSpriteInfo(index[i], SPRITE.INFO), SINFO.BEHIND_BG);
//            if (isBehind) {
//                behind = behind.below(spriteLine.get(i));
//            } else {
//                inFront = inFront.below(spriteLine.get(i));
//            }
//        }
        List<LcdImageLine> list = new ArrayList<>();
        list.add(behind);
        list.add(inFront);
        return list;
    }

    @Override
    public void write(int address, int data) {
        Preconditions.checkBits8(data);
        Preconditions.checkBits16(address);

        if (address == AddressMap.REG_DMA)
            System.out.println("activate dma");

        if (address >= AddressMap.VRAM_START && address < AddressMap.VRAM_END) {
            this.ramVideo.write(address - AddressMap.VRAM_START, data);
        }

        if (address >= AddressMap.REGS_START && address < AddressMap.REGS_END) {

            switch (address) {
            case AddressMap.REG_LCDC: {
                register.set(Reg.LCDC, data);
                if (!(Bits.test(data, 7))) {
                    this.changeMode(0);
                    this.modifyLYorLYC(Reg.LY, 0);
                    this.nextNonIdleCycle = Long.MAX_VALUE;
                }
            }
                break;

            case AddressMap.REG_LCD_STAT: {
                int newStat = (data & 0b11111000) | ((register.get(Reg.STAT) & 0b00000111));
                register.set(Reg.STAT, newStat);
            }
                break;

            case AddressMap.REG_LY: {
                // DO NOTHING
                // cf. 1.2.1 etape 9
                // ici car il ne faut pas rentrer dans le case défault
            }
                break;

            case AddressMap.REG_LYC: {
                this.modifyLYorLYC(Reg.LYC, data);
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
    
    private int getRegValue(int address) {
        Preconditions.checkBits16(address);
        int value = 0;
        switch (address) {
        case AddressMap.REG_LCDC:
            value = register.get(Reg.LCDC);
            break;
        case AddressMap.REG_LCD_STAT:
            value = register.get(Reg.STAT);
            break;
        case AddressMap.REG_SCY:
            value = register.get(Reg.SCY);
            break;
        case AddressMap.REG_SCX:
            value = register.get(Reg.SCX);
            break;
        case AddressMap.REG_LY:
            value = register.get(Reg.LY);
            break;
        case AddressMap.REG_LYC:
            value = register.get(Reg.LYC);
            break;
        case AddressMap.REG_DMA:
            value = register.get(Reg.DMA);
            break;
        case AddressMap.REG_BGP:
            value = register.get(Reg.BGP);
            break;
        case AddressMap.REG_OBP0:
            value = register.get(Reg.OBP0);
            break;
        case AddressMap.REG_OBP1:
            value = register.get(Reg.OBP1);
            break;
        case AddressMap.REG_WY:
            value = register.get(Reg.WY);
            break;
        case AddressMap.REG_WX:
            value = register.get(Reg.WX);
            break;
        }

        return value;
    }
    
    private void setRegValue(int address, int data) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);
        switch (address) {
        case AddressMap.REG_LCDC:
            register.set(Reg.LCDC, data);
            break;
        case AddressMap.REG_LCD_STAT:
            register.set(Reg.STAT, data);
            break;
        case AddressMap.REG_SCY:
            register.set(Reg.SCY, data);
            break;
        case AddressMap.REG_SCX:
            register.set(Reg.SCX, data);
            break;
        case AddressMap.REG_LY:
            register.set(Reg.LY, data);
            break;
        case AddressMap.REG_LYC:
            register.set(Reg.LYC, data);
            break;
        case AddressMap.REG_DMA:
            register.set(Reg.DMA, data);
            break;
        case AddressMap.REG_BGP:
            register.set(Reg.BGP, data);
            break;
        case AddressMap.REG_OBP0:
            register.set(Reg.OBP0, data);
            break;
        case AddressMap.REG_OBP1:
            register.set(Reg.OBP1, data);
            break;
        case AddressMap.REG_WY:
            register.set(Reg.WY, data);
            break;
        case AddressMap.REG_WX:
            register.set(Reg.WX, data);
            break;
        }
    }
    
    private boolean checkLyLyc() {
        return (register.get(Reg.LY) == register.get(Reg.LYC));
    }

    private void modifyLYorLYC(Reg reg, int data) {
        Preconditions.checkBits8(data);
        Preconditions.checkArgument(reg == Reg.LY || reg == Reg.LYC);
        register.set(reg, data);
        if (this.checkLyLyc()) {
            register.setBit(Reg.STAT, STAT.LYC_EQ_LY, true);
            if (Bits.test(register.get(Reg.STAT), STAT.INT_LYC)) {
                cpu.requestInterrupt(Interrupt.LCD_STAT);
            }
        } else {
            register.setBit(Reg.STAT, STAT.LYC_EQ_LY, false);
        }
    }
}
