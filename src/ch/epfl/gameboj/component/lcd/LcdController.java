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

    // Une tuile a une taille de 8 x 8 pixels
    private static final int TILE_SIZE = 8;
    // Taille de l'image de fond: 256 x 256 pixels, 32 x 32 tuiles
    private static final int BG_SIZE = 256;
    private static final int BG_TILE_SIZE = BG_SIZE / TILE_SIZE;
    // Résolution: 160 x 144, 20 x 18 tuiles
    public static final int LCD_WIDTH = 160, LCD_HEIGHT = 144;
    private static final int WIN_SIZE = BG_SIZE, WIN_TILE_SIZE = BG_TILE_SIZE;
    // L'abscisse de la fenêtre à l'écran est décalée de 7 pixels vers la droite
    private static final int WX_OFFSET = 7;
    // Durées en cycle des modes/opérations
    private static final int MODE2_DURATION = 20, MODE3_DURATION = 43, MODE0_DURATION = 51;
    public static final int LINE_CYCLE_DURATION = MODE2_DURATION + MODE3_DURATION + MODE0_DURATION;
    public static final int IMAGE_CYCLE_DURATION = 154 * LINE_CYCLE_DURATION;
    // L'image actuellement affichée par le contrôleur LCD
    private LcdImage displayedImage = BLANK_LCD_IMAGE;
    // Un contrôleur pour la VRAM
    private final VideoRamController videoRamController;
    // Un contrôleur pour la OAM
    private final OamRamController oamRamController;
    private final Cpu cpu;
    // Cet attribut stocke le prochain cycle où des opérations sont à effectuer
    private long nextNonIdleCycle = Long.MAX_VALUE;
    // Cet attribut représente l'index de la ligne de la fenêtre à être dessinée
    private int winY;
    // Ce constructeur permet de construire la prochaine image
    private LcdImage.Builder nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
    // Un contrôleur DMA permet d'assurer la copie rapide
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

    /**
     * Ce getter retourne l'image actuelle du contrôleur LCD.
     * @return
     */
    public LcdImage currentImage() {
        return Objects.requireNonNull(displayedImage, "Displayed image cannot be null");
    }

    @Override
    public void cycle(long cycle) {
        // Si le contrôleur était éteint et doit être rallumé, il est allumé
        if (nextNonIdleCycle == Long.MAX_VALUE && isOn()) {
            turnOn(cycle);
        }

        // Si la copie DMA est active, effectue la copie à raison d'un octet par cycle
        if (dmaController.isActive()) {
            dmaController.copy();
        }

        // Si le contrôleur est allumé et qu'il y a quelquechose à faire pendant ce cycle, effectue un cycle
        if (isOn() && cycle == nextNonIdleCycle) {
            // Le nombre de cycles depuis l'allumage
            long cycFromPowOn = cycle - lcdOnCycle; 
            // Le nombre de cycles depuis le début du dessin de l'image
            long cycFromImg = (int) (cycFromPowOn % IMAGE_CYCLE_DURATION);
            // L'index de la ligne en train d'être dessinée (entre 0 et 153)
            int currentLine = (int) (cycFromImg / LINE_CYCLE_DURATION);
            // Le nombre de cycles depuis le début du dessin de la ligne
            long cycFromLn = cycFromImg % LINE_CYCLE_DURATION;
            reallyCycle(currentLine, (int) cycFromLn);
        }
    }

    /**
     * Cette méthode gère les opérations à effectuer, elle n'est appelée que s'il y en a.
     * 
     * @param currentLine
     * la ligne actuellement en cours de dessin
     * @param cycFromLn
     * le nombre de cycles depuis le début du dessin de la ligne
     */
    private void reallyCycle(int currentLine, int cycFromLn) {        
        // Si la l'index de la ligne est celui d'une ligne "réelle", il faut passer par le cycle de dessin à trois sous-cycles
        if (currentLine < LCD_HEIGHT) {
            switch (cycFromLn) {
            case MODE2_DURATION:
                nextNonIdleCycle += MODE3_DURATION;
                setMode(3);
                break;
            case MODE2_DURATION + MODE3_DURATION:
                nextNonIdleCycle += MODE0_DURATION;
                setMode(0);
                // La ligne actuelle est effectivement dessinée lors du début du mode de dessin des sprites
                nextImageBuilder.setLine(currentLine, computeLine(currentLine));
                break;
            case 0:
                nextNonIdleCycle += MODE2_DURATION;
                // A la fin d'un cycle de dessin d'une ligne, l'index de la ligne est mis à jour
                modifyLYorLYC(LCDReg.LY, currentLine);
                setMode(2);
                break;
            }
        } else {
            if (currentLine == LCD_HEIGHT) {
                setMode(1);
                // Lors de l'entrée dans la période vertical blank (VBLANK, mode 1), l'image entière est mise à jour
                displayedImage = nextImageBuilder.build();
                nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
                // L'index de la ligne de la fenêtre est remis à zéro, afin de pouvoir recommencer lors de la prochaine image
                winY = 0;
            }
            
            nextNonIdleCycle += LINE_CYCLE_DURATION;
            modifyLYorLYC(LCDReg.LY, currentLine);
        }
    }

    /**
     * Cette méthode permet d'éteindre le contrôleur.
     */
    private void turnOff() {
        // Le mode est mis à 0 (cela est arbitrairement vrai dans les vraies GameBoy)
        setMode(0);
        // L'index de la ligne dessinée est forcé à 0
        modifyLYorLYC(LCDReg.LY, 0);
        nextNonIdleCycle = Long.MAX_VALUE;
    }

    /**
     * Cette méthode permet d'allumer/rallumer le contrôleur.
     * 
     * @param cycle
     * le cycle pendant lequel le rallumage se passe
     */
    private void turnOn(long cycle) {
        // Le cycle d'allumage est mis à jour
        lcdOnCycle = cycle;
        // Le prochain cycle "actif" devient celui de l'allumage
        nextNonIdleCycle = cycle;
        // Le contrôleur recommence instantanément à dessiner la prochaine image
        setMode(2);
    }

    private boolean isOn() {
        return lcdRegs.testBit(LCDReg.LCDC, LCDC.LCD_STATUS);
    }

    /**
     * Cette méthode permet de changer les modes de manière correcte, c'est à dire
     * en faisant attention aux interruptions (VBLANK et LCD_STAT).
     * 
     * @param mode
     * le mode dans lequel il faut passer
     */
    private void setMode(int mode) { 
        // La valeur du mode actuel est mise à jour
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
            // Lors du passage au mode 1, une interruption VBLANK est levée inconditionnellement
            cpu.requestInterrupt(Interrupt.VBLANK);
            break;
        case 2:
            if (lcdRegs.testBit(LCDReg.STAT, STAT.INT_MODE2)) {
                cpu.requestInterrupt(Interrupt.LCD_STAT);
            }
            break;
        }
    }

    /**
     * Cette méthode permet de calculer une ligne d'index donné.
     * 
     * @param lineIndex
     * l'index de la ligne
     * @return la ligne
     */
    private LcdImageLine computeLine(int lineIndex) {
        LcdImageLine nextLine = BLANK_LCD_IMAGE_LINE, fgSpriteLine = BLANK_LCD_IMAGE_LINE;

        int adjustedWX = lcdRegs.get(LCDReg.WX) - WX_OFFSET;
        adjustedWX = adjustedWX < 0 ? 0 : adjustedWX;
        
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
    	return adjustedWX < LCD_WIDTH && lcdRegs.testBit(LCDReg.LCDC, LCDC.WIN);
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
