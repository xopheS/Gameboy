package ch.epfl.gameboj.gui.color;

import ch.epfl.gameboj.bits.Bits;

public class JavaFXColor {
	public static final JavaFXColor STD_COLOR0 = new JavaFXColor(0xFF, 0xFF, 0xFF, 0xFF);
	public static final JavaFXColor STD_COLOR1 = new JavaFXColor(0xFF, 0xD3, 0xD3, 0xD3);
	public static final JavaFXColor STD_COLOR2 = new JavaFXColor(0xFF, 0xA9, 0xA9, 0xA9);
	public static final JavaFXColor STD_COLOR3 = new JavaFXColor(0xFF, 0x00, 0x00, 0x00);
	
	public static final JavaFXColor DARK_ORANGE = new JavaFXColor(0xFFFF8C00);
	public static final JavaFXColor DARK_RED = new JavaFXColor(0xFF8B0000);
	public static final JavaFXColor GREEN = new JavaFXColor(0xFF008000);
	public static final JavaFXColor TEAL = new JavaFXColor(0xFF008080);
	public static final JavaFXColor SADDLE_BROWN = new JavaFXColor(0xFF8B4513);
	public static final JavaFXColor MIDNIGHT_BLUE = new JavaFXColor(0xFF191970);
	public static final JavaFXColor LIGHT_STEEL_BLUE = new JavaFXColor(0xFFB0C4DE);
	public static final JavaFXColor INDIGO = new JavaFXColor(0xFF4B0082);
	public static final JavaFXColor IVORY = new JavaFXColor(0xFFFFFFF0);
	public static final JavaFXColor GOLDEN_ROD = new JavaFXColor(0xFFDAA520);
	public static final JavaFXColor FOREST_GREEN = new JavaFXColor(0xFF228B22);
	public static final JavaFXColor NAVY = new JavaFXColor(0xFF000080);

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
