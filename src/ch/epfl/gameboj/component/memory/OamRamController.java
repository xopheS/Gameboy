package ch.epfl.gameboj.component.memory;

import java.util.Arrays;
import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

public class OamRamController implements IRamController { 
    public enum DISPLAY_DATA {
        Y_COORD, X_COORD, TILE_INDEX, ATTRIBUTES
    }
    
    public enum ATTRIBUTES implements Bit {
        P_NUM0, P_NUM1, P_NUM2, VRAM_BANK, PALETTE, FLIP_H, FLIP_V, BEHIND_BG
    }
    
    public static final int OAM_SPRITES = 40;
    private static final int MAX_SPRITES = 10;
    public static final int SPRITE_ATTR_BYTES = 4;
    public static final int SPRITE_XOFFSET = 8;
    public static final int SPRITE_YOFFSET = 16;
    
    private IRamController ramController;
    
    public OamRamController(IRamController ramController) {
        this.ramController = Objects.requireNonNull(ramController);
    }
    
    public Integer[] spritesIntersectingLine(int lineIndex, int height) {
        int scanIndex = 0, foundSprites = 0;

        Integer[] intersect = new Integer[MAX_SPRITES];

        while (foundSprites < MAX_SPRITES && scanIndex < OAM_SPRITES) {
            int spriteY = readAttr(scanIndex, DISPLAY_DATA.Y_COORD) - SPRITE_YOFFSET;
            if (lineIndex >= spriteY && lineIndex < spriteY + height) {
                intersect[foundSprites] = packSpriteInfo(scanIndex);
                foundSprites++;
            }

            scanIndex++;
        }

        Integer[] intersectIndex = trimIntArray(intersect, foundSprites);

        Arrays.sort(intersectIndex); // TODO replace with System.sortarray call?

        return intersectIndex;
    }
    
    private Integer[] trimIntArray(Integer[] array, int trimIndex) {
        Integer[] intersectIndex = new Integer[trimIndex];

        for (int i = 0; i < trimIndex; ++i) {
            intersectIndex[i] = array[i];
        }

        return intersectIndex;
    }
    
    public int packSpriteInfo(int spriteIndex) {
        return Bits.make16(readAttr(spriteIndex, DISPLAY_DATA.X_COORD), spriteIndex);
    }
    
    public int readAttr(int spriteIndex, DISPLAY_DATA attr) {
        return read(AddressMap.OAM_START + Objects.checkIndex(spriteIndex, OAM_SPRITES) * SPRITE_ATTR_BYTES + attr.ordinal());
    }
    
    public boolean readAttr(int spriteIndex, ATTRIBUTES attribute) {
        return Bits.test(readAttr(spriteIndex, DISPLAY_DATA.ATTRIBUTES), attribute);
    }

    @Override
    public int read(int address) {
        return ramController.read(address);
    }

    @Override
    public void write(int address, int data) {
        ramController.write(address, data);
    }    
}
