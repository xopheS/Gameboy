package ch.epfl.gameboj.gui.color;

public class ColorTheme {
	public static final ColorTheme STANDARD_COLOR_THEME = new ColorTheme(JavaFXColor.STD_COLOR0, JavaFXColor.STD_COLOR1,
    		JavaFXColor.STD_COLOR2, JavaFXColor.STD_COLOR3);
	public static final ColorTheme CREEPY_COLOR_THEME = new ColorTheme(new JavaFXColor(0xFFA10684), new JavaFXColor(0xFF00FF00),
			new JavaFXColor(0xFFFF0921), new JavaFXColor(0xFFB3B191));
	public static final ColorTheme DESERT_COLOR_THEME = new ColorTheme(new JavaFXColor(0xFF25FDE9), new JavaFXColor(0xFFFCDC12),
			new JavaFXColor(0xFF3A9D23), new JavaFXColor(0xFF3F2204));
	public static final ColorTheme NIGHT_COLOR_THEME = new ColorTheme(new JavaFXColor(0xFF1B019B), new JavaFXColor(0xFFAFAFAF),
			new JavaFXColor(0xFF303030), new JavaFXColor(0xFF000000));
	
	private final JavaFXColor color0, color1, color2, color3;
	
	ColorTheme(JavaFXColor color0, JavaFXColor color1, JavaFXColor color2, JavaFXColor color3) {
		this.color0 = color0;
		this.color1 = color1;
		this.color2 = color2;
		this.color3 = color3;
	}
	
	public JavaFXColor getColor0() {
		return color0;
	}
	
	public JavaFXColor getColor1() {
		return color1;
	}
	
	public JavaFXColor getColor2() {
		return color2;
	}
	
	public JavaFXColor getColor3() {
		return color3;
	}
}
