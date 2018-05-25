package ch.epfl.gameboj.component.lcd;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Preconditions;

/**
 * Cette classe modélise un contrôleur DMA (Direct Memory Access), composant à
 * part entière de la GameBoy, qui permet d'effectuer une copie rapide (un
 * octet/cycle) de la RAM vers la VRAM/OAM du contrôleur LCD.
 * 
 * @author Christophe Saad (282557)
 * @author David Cian (287967)
 *
 */
public final class DmaController {
	private static final int COPY_LENGTH = 160;
	private static final DmaController dmaController = new DmaController();
	private Bus bus;
	private boolean isActive;
	private int startAddress;
	private int currentIndex;

	void setBus(Bus bus) {
		this.bus = bus;
	}

	/**
	 * Cette méthode initialise une copie rapide, à partir d'une certaine adresse.
	 * 
	 * @param addressMSB
	 *            l'octet des MSB de l'adresse de début de la copie
	 */
	void start(int addressMSB) {
		// Si une copie n'est pas déjà en cours, débute la copie
		if (!isActive) {
			startAddress = Preconditions.checkBits8(addressMSB) << Byte.SIZE;
			// D'après la documentation de la GameBoy, l'adresse ne peut être donnée que par
			// tranches de 256 (un octet non signé)
			Preconditions.checkArgument(startAddress % 0x100 == 0,
					"The start address can only be set in increments of 0x100");
			if (bus == null)
				throw new IllegalStateException("The bus has not been set");
			isActive = true;
		}
	}

	/**
	 * Cette méthode copie un octet à la fois de la RAM vers la VRAM/OAM du
	 * contrôleur LCD.
	 */
	void copy() {
		if (!isActive)
			throw new IllegalStateException("Cannot copy when quick copy is inactive");
		if (currentIndex < COPY_LENGTH) {
			bus.write(AddressMap.OAM_START + currentIndex, bus.read(startAddress + currentIndex));
			incrementIndex();
		}
	}

	/**
	 * Cette méthode incrémente l'index de la copie, en terminant la copie si elle a
	 * tout copié.
	 */
	private void incrementIndex() {
		if (++currentIndex == COPY_LENGTH) {
			end();
		}
	}

	/**
	 * Cette méthode met fin à la copie en remettant les attributs du contrôleur à
	 * leurs valeurs initiales.
	 */
	private void end() {
		isActive = false;
		currentIndex = 0;
		startAddress = 0;
	}

	/**
	 * Ce getter permet de se conformer au pattern singleton.
	 * 
	 * @return le contrôleur DMA
	 */
	static DmaController getDmaController() {
		return dmaController;
	}

	boolean isActive() {
		return isActive;
	}
}
