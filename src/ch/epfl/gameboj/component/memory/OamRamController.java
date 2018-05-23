package ch.epfl.gameboj.component.memory;

import java.util.Arrays;
import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

public class OamRamController extends RamController { 

    public enum DISPLAY_DATA { Y_COORD, X_COORD, TILE_INDEX, ATTRIBUTES }
    
    public enum ATTRIBUTES implements Bit { P_NUM0, P_NUM1, P_NUM2, VRAM_BANK, PALETTE, FLIP_H, FLIP_V, BEHIND_BG }
    
    public static final int MAX_SPRITES = 40;
    public static final int SPRITES_PER_LINE = 10;
    public static final int SPRITE_ATTR_BYTES = 4;
    public static final int SPRITE_XOFFSET = 8;
    public static final int SPRITE_YOFFSET = 16;
    
    public OamRamController(Ram ram, int startAddress) {
        super(ram, startAddress);
    }
    
    /**
     * Cette méthode calcule les sprites qui intersectent la ligne d'index donné.
     * @param lineIndex
     * l'index de la ligne
     * @param height
     * la hauteur des sprites
     * @return un tableau contenant les abscisses et les index des tuiles
     */
    public Integer[] spritesIntersectingLine(int lineIndex, int height) {
        int scanIndex = 0, foundSprites = 0;

        Integer[] intersect = new Integer[SPRITES_PER_LINE];

        while (foundSprites < SPRITES_PER_LINE && scanIndex < MAX_SPRITES) {
            int spriteY = readAttr(scanIndex, DISPLAY_DATA.Y_COORD) - SPRITE_YOFFSET;
            
            if (lineIndex >= spriteY && lineIndex < spriteY + height) {
                intersect[foundSprites] = packSpriteInfo(scanIndex);
                foundSprites++;
            }
            
            scanIndex++;
        }

        Integer[] intersectIndex = trimIntArray(intersect, foundSprites);

        Arrays.sort(intersectIndex);

        return intersectIndex;
    }
    
    private Integer[] trimIntArray(Integer[] array, int trimIndex) {
        Integer[] intersectIndex = new Integer[trimIndex];

        for (int i = 0; i < trimIndex; ++i) {
            intersectIndex[i] = array[i];
        }

        return intersectIndex;
    }
    
    private int packSpriteInfo(int spriteIndex) {
        return Bits.make16(readAttr(spriteIndex, DISPLAY_DATA.X_COORD), spriteIndex);
    }
    
    public int readAttr(int spriteIndex, DISPLAY_DATA attr) {
        return read(AddressMap.OAM_START 
            + Objects.checkIndex(spriteIndex, MAX_SPRITES) * SPRITE_ATTR_BYTES + attr.ordinal());
    }
    
    public boolean readAttr(int spriteIndex, ATTRIBUTES attribute) {
        return Bits.test(readAttr(spriteIndex, DISPLAY_DATA.ATTRIBUTES), attribute);
    }    
}
