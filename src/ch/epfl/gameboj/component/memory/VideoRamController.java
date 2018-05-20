package ch.epfl.gameboj.component.memory;

import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;

public class VideoRamController implements IRamController {
    public static final int BYTES_PER_TILE = 16;
    
    private IRamController ramController;
    
    public VideoRamController(IRamController ramController) {
        this.ramController = Objects.requireNonNull(ramController);
    }
    
    /**
     * Cette méthode permet d'obtenir un octet (msb ou lsb) dans les 384 tuiles du tileset.
     * @param tileTypeIndex
     * @param tileLineIndex
     * @param tileSource
     * @param msb
     * @return
     */
    public int tileLineBytes(int tileTypeIndex, int tileLineIndex, boolean tileSource, boolean msb) {
        Objects.checkIndex(tileTypeIndex, 384);
        return read(tileByteAddress(tileTypeIndex, tileLineIndex, tileSource) + (msb ? 1 : 0));
    }

    /**
     * Cette méthode permet d'obtenir un octet (msb ou lsb) dans les 384 tuiles du tileset, pour un sprite.
     * @param tileTypeIndex
     * @param tileLineIndex
     * @param tileSource
     * @param msb
     * @return
     */
    public int tileLineBytes(int tileTypeIndex, int tileLineIndex, boolean msb) {
        return tileLineBytes(tileTypeIndex, tileLineIndex, true, msb);
    }
    
    /**
     * Cette méthode permet d'obtenir l'adresse d'un octet correspondant au lsb ou msb d'une ligne donnée d'une tuile donnée.
     * @param tileTypeIndex
     * @param tileLineIndex
     * @param tileSource
     * @return
     */
    public int tileByteAddress(int tileTypeIndex, int tileLineIndex, boolean tileSource) {
        // TODO When double character composition, only even-numbered indexes can be
        // selected, when odd will be the same as even, how to do this?
        if (tileSource) {
            return AddressMap.TILE_SOURCE[1] + tileTypeIndex * BYTES_PER_TILE + tileLineIndex * 2;
        } else {
            if (tileTypeIndex >= 0 && tileTypeIndex < 128) {
                return AddressMap.TILE_SOURCE[0] + (tileTypeIndex + 128) * BYTES_PER_TILE + tileLineIndex * 2;
            } else if (tileTypeIndex >= 128 && tileTypeIndex < 256) {
                return AddressMap.TILE_SOURCE[0] + (tileTypeIndex - 128) * BYTES_PER_TILE + tileLineIndex * 2;
            } else {
                throw new IllegalArgumentException("tile_type_index wrong! " + tileTypeIndex);
            }
        }
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
