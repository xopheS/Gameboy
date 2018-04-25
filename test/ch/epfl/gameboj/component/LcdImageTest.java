/*
 *  @Author : Paul Juillard (288519)
 *  @Author : Leo Tafti (285418)
*/

package ch.epfl.gameboj.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.component.lcd.LcdImage;
import ch.epfl.gameboj.component.lcd.LcdImageLine;

public class LcdImageTest {
    
    private static LcdImage image32 = new LcdImage(new ArrayList<>(List.of(
            new LcdImageLine(new BitVector(32), new BitVector(32), new BitVector(32)),
            new LcdImageLine(new BitVector(32), new BitVector(32), new BitVector(32)))));
    private static LcdImage image64 = new LcdImage(new ArrayList<>(List.of(
            new LcdImageLine(new BitVector(64), new BitVector(64), new BitVector(64)),
            new LcdImageLine(new BitVector(64), new BitVector(64), new BitVector(64)))));
    
    private static LcdImageLine line32 = new LcdImageLine(new BitVector(32), new BitVector(32), new BitVector(32));
    private static LcdImageLine line64 = new LcdImageLine(new BitVector(64), new BitVector(64), new BitVector(64));
    /* ------------ Constructor tests ------------- */
    @Test
    public void constructorFailsOnInvalidArgs() {
        assertThrows(NullPointerException.class,                //null lines
                () -> new LcdImage(null));
        assertThrows(IllegalArgumentException.class,
                () -> new LcdImage(new ArrayList<>()));   // 0 height
        assertThrows(IllegalArgumentException.class,
                () -> new LcdImage(new ArrayList<>()));  // 0 width
        
        
        //invalid width
        assertThrows(IllegalArgumentException.class,
                () -> new LcdImage(new ArrayList<>()));
        
        //not matching width and lines
        assertThrows(IllegalArgumentException.class,
                () -> new LcdImage(new ArrayList<>(List.of(
                        new LcdImageLine(new BitVector(64), new BitVector(64), new BitVector(64))))));
        assertThrows(IllegalArgumentException.class,
                () -> new LcdImage( new ArrayList<>(List.of(
                        new LcdImageLine(new BitVector(32), new BitVector(32), new BitVector(32))))));
        
        //not matching height and lines
        assertThrows(IllegalArgumentException.class,
                () -> new LcdImage(new ArrayList<>(List.of(
                                new LcdImageLine(new BitVector(32), new BitVector(32), new BitVector(32)),
                                new LcdImageLine(new BitVector(32), new BitVector(32), new BitVector(32))))));
        assertThrows(IllegalArgumentException.class,
                () -> new LcdImage(new ArrayList<>(List.of(
                        new LcdImageLine(new BitVector(32), new BitVector(32), new BitVector(32))))));
    }

    @Test
    public void constructorWorksProperly() {
        //well, it's tested indirectly in all the other tests which construct an lcdImage
    }
    
    
    /* ------------ Builder tests ------------- */
    @Test
    public void builderConstructorFailsOnInvalidArgs() {
        //zero
        assertThrows(IllegalArgumentException.class,
                () -> new LcdImage.Builder(0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new LcdImage.Builder(32, 0));
        
        //x not multiple of 32
        assertThrows(IllegalArgumentException.class,
                () -> new LcdImage.Builder(10, 1));
        
        //negative
        assertThrows(IllegalArgumentException.class,
                () -> new LcdImage.Builder(-1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new LcdImage.Builder(64, -1));
    }
    
    @Test
    public void builderSetLineFailsOnInvalidArgs() {
        //not matching line pixel length
        assertThrows(IllegalArgumentException.class,
                () -> new LcdImage.Builder(32, 2)
                .setLine(0, line64));
        assertThrows(IllegalArgumentException.class,
                () -> new LcdImage.Builder(64, 2)
                .setLine(0, line32));
        
        //invalid index
        assertThrows(IndexOutOfBoundsException.class,
                () -> new LcdImage.Builder(32, 2)
                .setLine(-1, line32));
        assertThrows(IndexOutOfBoundsException.class,
                () -> new LcdImage.Builder(32, 2)
                .setLine(2, line32));
    }
    
    @Test
    public void builderWorksProperly() {
        LcdImage.Builder b = new LcdImage.Builder(32, 2);
        b.setLine(0, line32);
        b.setLine(1, line32);
        LcdImage image = b.build();

        LcdImage control = new LcdImage(
                new ArrayList<>(List.of(line32, line32)));
        assertEquals(control, image);

        LcdImage.Builder d = new LcdImage.Builder(64, 2);
        d.setLine(0, line64);
        d.setLine(1, line64);
        LcdImage image2 = d.build();

        LcdImage control2 = new LcdImage(
                new ArrayList<>(List.of(line64, line64)));
        assertEquals(control2, image2);
    }
    
    /* ------------ Getters tests ------------- */
    @Test
    public void widthWorksProperly() {
        assertEquals(32, image32.width());
        assertEquals(64, image64.width());
    }
    
    @Test
    public void heightWorksProperly() {
        assertEquals(2, image32.height());
        assertEquals(2, image64.height());
    }
    
    /* ------------ Color get() tests ------------- */
    @Test
    public void getFailsOnInvalidArgs() {
        //invalid x
        assertThrows(IndexOutOfBoundsException.class,
                () -> image32.get(-1, 0));
        assertThrows(IndexOutOfBoundsException.class,
                () -> image32.get(32, 0));
        
        //invalid y
        assertThrows(IndexOutOfBoundsException.class,
                () -> image32.get(0, -1));
        assertThrows(IndexOutOfBoundsException.class,
                () -> image32.get(0, 2));
                
    }
    
    @Test
    public void getWorksProperly() {
        for(int j = 0; j < image64.height(); j++) {
            for(int i = 0; i < image64.width(); i++) {
                assertEquals(0, image64.get(i, j));
            }
        }
        
        BitVector msb = new BitVector.Builder(64)
                .setByte(0, 0b0000_0001)
                .setByte(4, 0b0000_0001)
                .build();
        
        LcdImage image = new LcdImage( new ArrayList<>(List.of(
                new LcdImageLine(new BitVector(32), new BitVector(32), new BitVector(32)),
                new LcdImageLine(msb, new BitVector(32), new BitVector(32)))));
        
        for(int j = 0; j < image.height(); j++) {
            for(int i = 0; i < image.width(); i++) {
                if(i == 0)
                    assertEquals(0b10, image.get(i, j));
                else
                    assertEquals(0, image.get(i, j));
            }
        }
    }
    
    
    /* ------------ equals() and hashCode() tests ------------- */
    @Test
    public void equalsWorksProperly() {
        LcdImage otherDifferentHeight = new LcdImage(new ArrayList<>(List.of(
                new LcdImageLine(new BitVector(64), new BitVector(64), new BitVector(64)))));
        
        LcdImage otherDifferentWidth = new LcdImage( new ArrayList<>(List.of(
                new LcdImageLine(new BitVector(32), new BitVector(32), new BitVector(32)),
                new LcdImageLine(new BitVector(32), new BitVector(32), new BitVector(32)))));
        
        LcdImage otherButSame = new LcdImage(new ArrayList<>(List.of(
                new LcdImageLine(new BitVector(64), new BitVector(64), new BitVector(64)),
                new LcdImageLine(new BitVector(64), new BitVector(64), new BitVector(64)))));
        
        assertFalse(image64.equals(new Object()));
        assertFalse(image64.equals(otherDifferentHeight));
        assertFalse(image64.equals(otherDifferentWidth));
        
        assertTrue(image64.equals(image64));
        assertTrue(image64.equals(otherButSame));
        assertTrue(otherButSame.equals(image64));
    }
    
    @Test
    public void hashCodeWorksProperly() {
        LcdImage otherDifferentHeight = new LcdImage(new ArrayList<>(List.of(
                new LcdImageLine(new BitVector(64), new BitVector(64), new BitVector(64)))));
        
        LcdImage otherDifferentWidth = new LcdImage(new ArrayList<>(List.of(
                new LcdImageLine(new BitVector(32), new BitVector(32), new BitVector(32)),
                new LcdImageLine(new BitVector(32), new BitVector(32), new BitVector(32)))));
        
        LcdImage otherButSame = new LcdImage(new ArrayList<>(List.of(
                new LcdImageLine(new BitVector(64), new BitVector(64), new BitVector(64)),
                new LcdImageLine(new BitVector(64), new BitVector(64), new BitVector(64)))));
        
        assertFalse(image64.hashCode() == (new Object()).hashCode());
        assertFalse(image64.hashCode() == otherDifferentHeight.hashCode());
        assertFalse(image64.hashCode() == otherDifferentWidth.hashCode());
        
        assertTrue(image64.hashCode() == otherButSame.hashCode());
    }
}
