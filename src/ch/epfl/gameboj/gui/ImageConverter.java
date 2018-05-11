package ch.epfl.gameboj.gui;

import static ch.epfl.gameboj.component.lcd.LcdController.LCD_WIDTH;
import static ch.epfl.gameboj.component.lcd.LcdController.LCD_HEIGHT;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.lcd.LcdImage;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;

public final class ImageConverter {
    
    private static class JavaFXColor {       
        private int alpha, red, green, blue;
        
        JavaFXColor(int alpha, int red, int green, int blue) {
            this.alpha = alpha;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }
    } //nested class or enum? TODO
    
    JavaFXColor[] fxColors = new JavaFXColor[] { new JavaFXColor(0xFF, 0xFF, 0xFF, 0xFF), new JavaFXColor(0xFF, 0xD3, 0xD3, 0xD3),
            new JavaFXColor(0xFF, 0xA9, 0xA9, 0xA9), new JavaFXColor(0xFF, 0x00, 0x00, 0x00)
    };
    
    public static Image convert(LcdImage lcdImage) {
        Preconditions.checkArgument(lcdImage.getWidth() == LCD_WIDTH && lcdImage.getHeight() == LCD_HEIGHT);
        
        WritableImage wImage = new WritableImage(LCD_WIDTH, LCD_HEIGHT);
        PixelWriter pWriter = wImage.getPixelWriter();
        
        for (int y = 0; y < lcdImage.getHeight(); ++y) {
            
            for (int x = 0; x < lcdImage.getWidth(); ++x) {
                
                int color = 0;
                
                switch(lcdImage.get(x, y)) {
                  case 0 : color = 0xFFFFFFFF; break;
                  case 1 : color = 0xFFD3D3D3; break;
                  case 2 : color = 0xFFA9A9A9; break;
                  case 3 : color = 0xFF000000; break;
                }
                
                pWriter.setArgb(x, y, color);
            }
        }
        
        return wImage; 
    }
}
