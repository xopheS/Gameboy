package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.AddressMap;

public class VideoRamController extends RamController {

    public static final int BYTES_PER_TILE = 16;
    public static final int TILE_SOURCE_HALF = 128;
    
    public VideoRamController(Ram ram, int startAddress) {
        super(ram, startAddress);
    }
    
    /**
     * Cette méthode permet d'obtenir un octet (msb ou lsb) dans les 384 tuiles du tileset.
     * @param tileTypeIndex
     * l'index de la tuile dans le tileset
     * @param tileLineIndex
     * l'index de la ligne au sein de la tuile
     * @param tileSource
     * la source de la tuile
     * @return
     */
    public int[] tileLineBytes(int tileTypeIndex, int tileLineIndex, boolean tileSource) {
        int tileByteAddress = tileByteAddress(tileTypeIndex, tileLineIndex, tileSource);
        return new int[] { read(tileByteAddress), read(tileByteAddress + 1) };
    }

    /**
     * Cette méthode permet d'obtenir un octet (msb ou lsb) dans les 384 tuiles du tileset, pour un sprite.
     * 
     * @param tileTypeIndex
     * l'index de la tuile dans le tileset
     * @param tileLineIndex
     * l'index de la ligne au sein de la tuile
     * @return
     */
    public int[] tileLineBytes(int tileTypeIndex, int tileLineIndex) {
        return tileLineBytes(tileTypeIndex, tileLineIndex, true);
    }
    
    /**
     * Cette méthode permet d'obtenir l'adresse d'un octet correspondant au lsb ou msb d'une ligne donnée d'une tuile donnée.
     * 
     * @param tileTypeIndex
     * l'index de la tuile dans le tileset
     * @param tileLineIndex
     * l'index de la ligne au sein de la tuile
     * @param tileSource
     * la source de la tuile
     * @return
     */
    private int tileByteAddress(int tileTypeIndex, int tileLineIndex, boolean tileSource) {
        if (tileSource) {
            return AddressMap.TILE_SOURCE[1] + tileTypeIndex * BYTES_PER_TILE + 2 * tileLineIndex;
        } else {
            if (tileTypeIndex >= 0 && tileTypeIndex < TILE_SOURCE_HALF) {
                return AddressMap.TILE_SOURCE[0] + (tileTypeIndex + TILE_SOURCE_HALF) * BYTES_PER_TILE + 2 * tileLineIndex;
            } else if (tileTypeIndex >= 128 && tileTypeIndex < 256) {
                return AddressMap.TILE_SOURCE[0] + (tileTypeIndex - TILE_SOURCE_HALF) * BYTES_PER_TILE + 2 * tileLineIndex;
            } else {
                throw new IllegalArgumentException("tile_type_index wrong! " + tileTypeIndex);
            }
        }
    }
}
