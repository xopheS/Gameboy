package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.AddressMap;


/**
 * Cette classe représente le controlleur de la mémoire video du controlleur lcd
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 */

public class VideoRamController extends RamController {

    public static final int BYTES_PER_TILE = 16;
    public static final int TILE_SOURCE_SIZE = 256;
    public static final int TILE_SOURCE_HALF = TILE_SOURCE_SIZE / 2;
    
    /**
     * Construit un contrôleur pour la mémoire video donnée en argument, accessible à
     * partir de l'adresse donnée
     * 
     * @param ram
     *          la mémoire video
     * @param startAddress
     *          l'adresse de début
     */
    public VideoRamController(Ram ram, int startAddress) {
        super(ram, startAddress);
    }
    
  /**
   * Cette méthode permet d'obtenir un octet (msb ou lsb) dans les 384 tuiles du tileset.
   * 
   * @param tileTypeIndex
   *          l'index de la tuile dans le tileset
   * @param tileLineIndex
   *          l'index de la ligne au sein de la tuile
   * @param tileSource
   *          la source de la tuile
   * @return
   */
    public int[] tileLineBytes(int tileTypeIndex, int tileLineIndex, boolean tileSource) {
        int tileByteAddress = tileByteAddress(tileTypeIndex, tileLineIndex, tileSource);
        return new int[] { read(tileByteAddress), read(tileByteAddress + 1) };
    }

  /**
   * Cette méthode permet d'obtenir un octet (msb ou lsb) dans les 384 tuiles du tileset, pour un
   * sprite.
   * 
   * @param tileTypeIndex
   *          l'index de la tuile dans le tileset
   * @param tileLineIndex
   *          l'index de la ligne au sein de la tuile
   * @return
   */
    public int[] tileLineBytes(int tileTypeIndex, int tileLineIndex) {
        return tileLineBytes(tileTypeIndex, tileLineIndex, true);
    }
    
  /**
   * Cette méthode permet d'obtenir l'adresse d'un octet correspondant au lsb ou msb d'une ligne
   * donnée d'une tuile donnée.
   * 
   * @param tileTypeIndex
   *          l'index de la tuile dans le tileset
   * @param tileLineIndex
   *          l'index de la ligne au sein de la tuile
   * @param tileSource
   *          la source de la tuile
   * @return
   */
    private int tileByteAddress(int tileTypeIndex, int tileLineIndex, boolean tileSource) {
        if (tileSource) {
            return AddressMap.TILE_SOURCE[1] + tileTypeIndex * BYTES_PER_TILE + 2 * tileLineIndex;
        } else {
            if (tileTypeIndex >= 0 && tileTypeIndex < TILE_SOURCE_HALF) {
                return AddressMap.TILE_SOURCE[0] + (tileTypeIndex + TILE_SOURCE_HALF) * BYTES_PER_TILE + 2 * tileLineIndex;
            } else if (tileTypeIndex >= TILE_SOURCE_HALF && tileTypeIndex < TILE_SOURCE_SIZE) {
                return AddressMap.TILE_SOURCE[0] + (tileTypeIndex - TILE_SOURCE_HALF) * BYTES_PER_TILE + 2 * tileLineIndex;
            } else {
                throw new IllegalArgumentException("tile_type_index wrong! " + tileTypeIndex);
            }
        }
    }
}
