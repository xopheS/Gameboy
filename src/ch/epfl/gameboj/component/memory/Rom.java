package ch.epfl.gameboj.component.memory;

import java.util.Arrays;
import java.util.Objects;

import ch.epfl.gameboj.component.cartridge.Saveable;

/**
 * Rom : une mémoire morte à contenu immuable.
 *
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 */

public final class Rom implements Saveable {

    private final byte[] data;

    /**
     * Constructeur qui construit une mémoire morte dont le contenu et la taille
     * sont ceux du tableau d'octets donné en argument.
     *
     * @param data
     *            tableau qui constitue la mémoire morte
     * @throws NullPointerException
     *             si l'argument est nul
     */
    public Rom(byte[] data) {
        this.data = Arrays.copyOf(Objects.requireNonNull(data), data.length);
    }

    /**
     * Retourne la taille en octets de la mémoire (taille du tableau de byte).
     *
     * @return data.length la taille de la mémoire
     */
    public int size() {
        return data.length;
    }

    /**
     * Retourne l'octet se trouvant à l'index donné, sous la forme d'une valeur
     * comprise entre 0 et 0xFF.
     *
     * @param index
     *            l'index de la donnée à lire
     * @return entier entre 0 et 0xFF
     * @throws IndexOutOfBoundsException
     *             si l'index est invalide (négatif ou supérieur à la taille du
     *             tableau - 1)
     */
    public int read(int index) {
        return Byte.toUnsignedInt(data[Objects.checkIndex(index, data.length)]);
    }

	@Override
	public byte[] getByteArray() {
		return data;
	}

	@Override
	public void setByteArray(byte[] byteArray) {
		// TODO Auto-generated method stub
		
	}
}
