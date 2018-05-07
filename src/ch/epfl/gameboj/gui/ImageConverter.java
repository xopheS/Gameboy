package ch.epfl.gameboj.gui;

import ch.epfl.gameboj.component.lcd.LcdController;
import ch.epfl.gameboj.component.lcd.LcdImage;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;

public class ImageConverter {
    
    public static Image convert(LcdImage image){
        
        WritableImage wImage = new WritableImage(LcdController.LCD_WIDTH, LcdController.LCD_HEIGHT);
        PixelWriter pWriter = wImage.getPixelWriter();
        
        for (int y = 0; y < image.getHeight(); ++y) {
            
            for (int x = 0; x < image.getWidth(); ++x) {
                
                int color = 0;
                switch(image.get(x, y)) {
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
