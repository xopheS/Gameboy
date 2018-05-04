package ch.epfl.gameboj;

import ch.epfl.gameboj.component.Component;

public class SimpleComponent implements Component {
    private final int address;
    private int value;
    private boolean wasRead, wasWritten;

    SimpleComponent(int address, int initialValue) {
        this.address = address;
        this.value = initialValue;
    }

    boolean wasRead() { 
        return wasRead; 
    }
    
    boolean wasWritten() { 
        return wasWritten; 
    }

    @Override
    public int read(int a) {
        wasRead = true;
        return a == address ? value : Component.NO_DATA;
    }

    @Override
    public void write(int a, int d) {
        wasWritten = true;
        if (a == address) {
            value = d;
        }
    }
}
