package ch.epfl.gameboj.component.lcd;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.component.lcd.LcdImage;
import ch.epfl.gameboj.component.lcd.LcdImageLine;

public class LcdTests {

    BitVector msb = new BitVector(32, true);
    BitVector lsb = new BitVector(32, false);
    BitVector opacity = new BitVector(32, true);
    LcdImageLine a = new LcdImageLine(msb, lsb, opacity);

    BitVector msbb = new BitVector(32, false);
    BitVector lsbb = new BitVector(32, false);
    BitVector opacityy = new BitVector.Builder(32).setByte(0, 0b1111_0000)
            .setByte(1, 0b1010_1010).setByte(3, 0b1100_1100).build();
    // 11001100000000001010101011110000
    LcdImageLine b = new LcdImageLine(msbb, lsbb, opacityy);

    BitVector msbbb = new BitVector.Builder(32).setByte(0, 0b1111_0000)
            .setByte(1, 0b1010_1010).setByte(3, 0b1100_1100).build();
    // 11001100000000001010101011110000
    BitVector lsbbb = new BitVector.Builder(32).setByte(0, 0b0000_0110)
            .setByte(1, 0b1010_1110).setByte(3, 0b1100_0000).build();
    // 11000000000000001010111000000110
    BitVector opacityyy = new BitVector(32, true);
    LcdImageLine c = new LcdImageLine(msbbb, lsbbb, opacityyy);

    BitVector msbbbb = new BitVector(32, false);
    BitVector lsbbbb = new BitVector(32, true);
    BitVector opacityyyy = new BitVector(32, false);
    LcdImageLine d = new LcdImageLine(msbbbb, lsbbbb, opacityyyy);

    @Test
    void shiftTest() {

        LcdImageLine c = a.shift(3);
        LcdImageLine d = a.shift(-5);
        assertEquals("11111111111111111111111111111000", c.getMsb().toString());
        assertEquals("00000000000000000000000000000000", c.getLsb().toString());
        assertEquals("11111111111111111111111111111000",
                c.getOpacity().toString());
        assertEquals("00000111111111111111111111111111", d.getMsb().toString());
        assertEquals("00000000000000000000000000000000", d.getLsb().toString());
        assertEquals("00000111111111111111111111111111",
                d.getOpacity().toString());

    }

    @Test
    void colorTest() {

        LcdImageLine v1 = c.mapColors(0b11100100);
        assertEquals(v1, c);
        LcdImageLine v2 = c.mapColors(0b01001110);
        //            11001100000000001010101011110000
        assertEquals("00110011111111110101010100001111", v2.getMsb().toString());
        //            11000000000000001010111000000110
        assertEquals("11000000000000001010111000000110", v2.getLsb().toString());
        
        


    }

    @Test
    void below1() {

        LcdImageLine v1 = a.below(b);
        assertEquals("11111111111111111111111111111111",
                v1.getOpacity().toString());
        assertEquals("00110011111111110101010100001111", v1.getMsb().toString());
        assertEquals("00000000000000000000000000000000", v1.getLsb().toString());
        // 11001100000000001010101011110000

    }

    @Test
    void below2() {
        BitVector z = new BitVector.Builder(32).setByte(0, 0b0000_0110)
                .setByte(1, 0b1010_1110).setByte(3, 0b1100_0000).build();
        // 11000000000000001010111000000110
        LcdImageLine v1 = a.below(b, z);
        assertEquals("11111111111111111111111111111111",
                v1.getOpacity().toString());
        assertEquals("00111111111111110101000111111001", v1.getMsb().toString());
        assertEquals("00000000000000000000000000000000", v1.getLsb().toString());
        // 11000000000000001010111000000110

    }

    @Test

    void join() {

        LcdImageLine v1 = a.join(d, 4);

        assertEquals("00000000000000000000000000001111", v1.getMsb().toString());
        assertEquals("11111111111111111111111111110000", v1.getLsb().toString());
        assertEquals("00000000000000000000000000001111",
                v1.getOpacity().toString());

    }
    
    @Test
    void colorsMapWork1() {
        BitVector.Builder bvb = new BitVector.Builder(32);
        bvb.setByte(0, 0b00000000);
        bvb.setByte(1, 0b00000000);
        bvb.setByte(2, 0b11111111);
        bvb.setByte(3, 0b11111111);
        BitVector bv1 = bvb.build();
        bvb = new BitVector.Builder(32);
        bvb.setByte(0, 0b00000000);
        bvb.setByte(1, 0b11111111);
        bvb.setByte(2, 0b00000000);
        bvb.setByte(3, 0b11111111);
        BitVector bv2 = bvb.build();

        LcdImageLine lcdLine = new LcdImageLine(bv1, bv2, new BitVector(32));
        LcdImageLine result1 = lcdLine.mapColors(0b10110100);
        LcdImageLine result2 = lcdLine.mapColors(0b11111111);
        LcdImageLine result3 = lcdLine.mapColors(0b01011011);

        assertEquals("11111111111111110000000000000000",
                result1.getMsb().toString());
        assertEquals("00000000111111111111111100000000",
                result1.getLsb().toString());
        assertEquals("11111111111111111111111111111111",
                result2.getLsb().toString());
        assertEquals(result2.getMsb(), result2.getLsb());
        assertEquals("00000000000000001111111111111111",
                result3.getMsb().toString());
        assertEquals("11111111111111110000000011111111",
                result3.getLsb().toString());
    }

    @Test
    void equalsAndHashCodeWork1() {
        BitVector msb = new BitVector(4 * 32, true);
        BitVector lsb = new BitVector(4 * 32);
        BitVector opa = BitVector.rand();
        BitVector msb2 = new BitVector(4*32, true);
        LcdImageLine line1 = new LcdImageLine(msb, lsb, opa);
        LcdImageLine line2 = new LcdImageLine(msb2, lsb, opa);
        assertTrue(line1.equals(line2));
        assertEquals(line1.hashCode(), line2.hashCode());
    }


    
    @Test
    void colorsMapWork() {
        BitVector.Builder bvb = new BitVector.Builder(32);
        bvb.setByte(0, 0b00000000);
        bvb.setByte(1, 0b00000000);
        bvb.setByte(2, 0b11111111);
        bvb.setByte(3, 0b11111111);
        BitVector bv1 = bvb.build();
        bvb = new BitVector.Builder(32);
        bvb.setByte(0, 0b00000000);
        bvb.setByte(1, 0b11111111);
        bvb.setByte(2, 0b00000000);
        bvb.setByte(3, 0b11111111);
        BitVector bv2 = bvb.build();

        LcdImageLine lcdLine = new LcdImageLine(bv1, bv2, new BitVector(32));
        LcdImageLine result1 = lcdLine.mapColors(0b10110100);
        LcdImageLine result2 = lcdLine.mapColors(0b11111111);
        LcdImageLine result3 = lcdLine.mapColors(0b01011011);

        assertEquals("11111111111111110000000000000000",
                result1.getMsb().toString());
        assertEquals("00000000111111111111111100000000",
                result1.getLsb().toString());
        assertEquals("11111111111111111111111111111111",
                result2.getLsb().toString());
        assertEquals(result2.getMsb(), result2.getLsb());
        assertEquals("00000000000000001111111111111111",
                result3.getMsb().toString());
        assertEquals("11111111111111110000000011111111",
                result3.getLsb().toString());
    }

    @Test
    void equalsAndHashCodeWork() {
        BitVector msb = new BitVector(4 * 32, true);
        BitVector lsb = new BitVector(4 * 32);
        BitVector opa = BitVector.rand();
        BitVector msb2 = new BitVector(4*32, true);
        LcdImageLine line1 = new LcdImageLine(msb, lsb, opa);
        LcdImageLine line2 = new LcdImageLine(msb2, new BitVector(lsb), new BitVector(opa));
        assertTrue(msb.equals(msb2));
        assertEquals(line1.hashCode(), line2.hashCode());
    }
    
    
    @Test
    void getColorWorks() {
        List<LcdImageLine> list = new ArrayList<>();
        BitVector.Builder bvb = new BitVector.Builder(32);
        bvb.setByte(0, 0b00000000).setByte(1, 0b00000000).setByte(2, 0b11111111).setByte(3, 0b11111111);
        BitVector b1 = bvb.build();
        //11111111111111110000000000000000
        
        bvb = new BitVector.Builder(32);
        bvb.setByte(0, 0b00001111).setByte(1, 0b00011000).setByte(2, 0b00011111).setByte(3, 0b11110111);
        BitVector b2 = bvb.build();
        //11110111000111110001100000001111
        
        bvb = new BitVector.Builder(32);
        bvb.setByte(0, 0b10001111).setByte(1, 0b11001000).setByte(2, 0b01011111).setByte(3, 0b00110111);
        BitVector b3 = bvb.build();
        //00110111010111111100100010001111
        
        list.add(new LcdImageLine(b1, b2, b3));
        list.add(new LcdImageLine(b1, b2, b3));
        list.add(new LcdImageLine(b1, b2, b3));
        list.add(new LcdImageLine(b1, b2, b3));
        
        LcdImage im = new LcdImage(32, 4, list);
        assertEquals(1, im.get(1, 0));
        assertEquals(0, im.get(8, 0));
        assertEquals(3, im.get(31, 3));
        
    }
    
    @Test
    void LcdImageEqualsWorks() {
        List<LcdImageLine> list = new ArrayList<>();
        BitVector.Builder bvb = new BitVector.Builder(32);
        bvb.setByte(0, 0b00000000).setByte(1, 0b00000000).setByte(2, 0b11111111).setByte(3, 0b11111111);
        BitVector b1 = bvb.build();
        //11111111111111110000000000000000
        
        bvb = new BitVector.Builder(32);
        bvb.setByte(0, 0b00001111).setByte(1, 0b00011000).setByte(2, 0b00011111).setByte(3, 0b11110111);
        BitVector b2 = bvb.build();
        //11110111000111110001100000001111
        
        bvb = new BitVector.Builder(32);
        bvb.setByte(0, 0b10001111).setByte(1, 0b11001000).setByte(2, 0b01011111).setByte(3, 0b00110111);
        BitVector b3 = bvb.build();
        //00110111010111111100100010001111
        
        list.add(new LcdImageLine(b1, b2, b3));
        list.add(new LcdImageLine(b1, b2, b3));
        list.add(new LcdImageLine(b1, b2, b3));
        list.add(new LcdImageLine(b1, b2, b3));
        
        LcdImage im = new LcdImage(32, 4, list);
        LcdImage im1 = new LcdImage(32, 4, list);
        assertTrue(im.equals(im1));
    }
}
