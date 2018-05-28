package ch.epfl.gameboj.gui.color;

import ch.epfl.gameboj.bits.Bits;

public class JavaFXColor {
	public static final JavaFXColor STD_COLOR0 = new JavaFXColor(0xFF, 0xFF, 0xFF, 0xFF);
	public static final JavaFXColor STD_COLOR1 = new JavaFXColor(0xFF, 0xD3, 0xD3, 0xD3);
	public static final JavaFXColor STD_COLOR2 = new JavaFXColor(0xFF, 0xA9, 0xA9, 0xA9);
	public static final JavaFXColor STD_COLOR3 = new JavaFXColor(0xFF, 0x00, 0x00, 0x00);

    private int alpha, red, green, blue;
    
    JavaFXColor(int argb) {
    	this.alpha = Bits.extract(argb, 3 * Byte.SIZE, Byte.SIZE);
    	this.red = Bits.extract(argb, 2*Byte.SIZE, Byte.SIZE);
    	this.green = Bits.extract(argb, Byte.SIZE, Byte.SIZE);
    	this.blue = Bits.extract(argb, 0, Byte.SIZE);
    }

    JavaFXColor(int alpha, int red, int green, int blue) {
        this.alpha = alpha;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }
    
    void setAlpha(int i) {
    	this.alpha = i;
    }
    
    void setRed(int red) {
    	this.red = red;
    }
    
    void setGreen(int green) {
    	this.green = green;
    }
    
    void setBlue(int blue) {
    	this.blue = blue;
    }

    public int getARGB() {
        return alpha << Byte.SIZE * 3 | red << Byte.SIZE * 2 | green << Byte.SIZE | blue;
    }       
}
