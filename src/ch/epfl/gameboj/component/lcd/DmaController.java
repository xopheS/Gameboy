package ch.epfl.gameboj.component.lcd;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Preconditions;

public final class DmaController {
    public final static int COPY_LENGTH = 160; //XXX set private
    private final static DmaController quickCopy = new DmaController();
    private static Bus bus;
    private boolean isActive = false;
    private int startAddress;
    private int currentIndex = 0;
    
    private DmaController() {
        
    }
    
    void setBus(Bus bus) {
        this.bus = bus;
    }
    
    void start(int addressMSB) {
        if (bus == null) throw new IllegalStateException("The bus has not been set");
        //if (isActive) throw new IllegalStateException("A quick copy is already taking place"); TODO: can a quick copy be initiated while another one is going on?
        isActive = true;
        startAddress = Preconditions.checkBits8(addressMSB) << Byte.SIZE;
    }
    
    void copy() {
        if (!isActive) throw new IllegalStateException("Cannot copy when quick copy is inactive");
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
    
    static DmaController getQuickCopyUtil() {
        return quickCopy;
    }
    
    boolean isActive() {
        return isActive;
    }
}
