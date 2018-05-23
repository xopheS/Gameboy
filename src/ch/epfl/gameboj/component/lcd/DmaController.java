package ch.epfl.gameboj.component.lcd;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Preconditions;

/**
 * Cette classe modélise le contolleur de l'accès direct à la mémoire DMA.
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

    
    /**
     * Cette méthode fournit un bus au controlleur DMA
     * @param bus le bus
     */
    public void setBus(Bus bus) {
        this.bus = bus;
    }

    
  /**
   * Cette méthdode initie une copie rapide avec l'adresse donnée en argument
   * 
   * @param addressMSB
   *          l'addresse de la copie
   */
    void start(int addressMSB) {
      if (!isActive) {
        startAddress = Preconditions.checkBits8(addressMSB) << Byte.SIZE;
        Preconditions.checkArgument(startAddress % 0x100 == 0,
            "The start address can only be set in increments of 0x100");
        if (bus == null)
          throw new IllegalStateException("The bus has not been set");
        isActive = true;
      }
    }

    /**
     * Cette méthode effectue la copie rapide
     */
    void copy() {
        if (!isActive)
            throw new IllegalStateException("Cannot copy when quick copy is inactive");
        if (currentIndex < COPY_LENGTH) {
            bus.write(AddressMap.OAM_START + currentIndex, bus.read(startAddress + currentIndex));
            incrementIndex();
        }
    }

    private void incrementIndex() {
        if (++currentIndex == COPY_LENGTH) {
            end();
        }
    }

    private void end() {
        isActive = false;
        currentIndex = 0;
        startAddress = 0;
    }

    
  /**
   * Cette méthode retourne le controlleur DMA
   * 
   * @return controlleur DMA
   */
    static DmaController getDmaController() {
        return dmaController;
    }

    
  /**
   * Cette méthode indique si l'a copie rapide est activée
   * 
   * @return si l'a copie rapide est activée
   */
    boolean isActive() {
        return isActive;
    }
}
