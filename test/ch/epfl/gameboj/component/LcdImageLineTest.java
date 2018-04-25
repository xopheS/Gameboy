/*
 *  @Author : Paul Juillard (288519)
 *  @Author : Leo Tafti (285418)
*/

package ch.epfl.gameboj.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.Test;

import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.BitVector.Builder;
import ch.epfl.gameboj.component.lcd.LcdImageLine;

public class LcdImageLineTest {
    
//    System.out.println(l.msb());
//    System.out.println(l.lsb());
//    System.out.println(l.opacity());
    

    // -------------------- CONSTRUCTOR TEST --------------------
   
    @Test
    public void LcdImageLineFailOnDifferentSizeBitVector() {
        assertThrows(IllegalArgumentException.class, 
                () -> new LcdImageLine(bit64_0(), bit64_0(), allZeros()));
        assertThrows(IllegalArgumentException.class, 
                () -> new LcdImageLine( bit64_0(), allZeros(), bit64_0()));
        assertThrows(IllegalArgumentException.class, 
                () -> new LcdImageLine( allZeros(), bit64_0(),  bit64_0()));
    }
    
    // -------------------- BUILDER TEST     --------------------
       
    @Test
    public void BuilderConstructorsFailOnInvalidSize() {
        assertThrows(IllegalArgumentException.class, 
                () -> new LcdImageLine.Builder(-1));
        assertThrows(IllegalArgumentException.class, 
                () -> new LcdImageLine.Builder(0));
        assertThrows(IllegalArgumentException.class, 
                () -> new LcdImageLine.Builder(31));
        assertThrows(IllegalArgumentException.class, 
                () -> new LcdImageLine.Builder(-32));
    }
    
    @Test
    public void BuilderSizeWorks() {
        LcdImageLine.Builder b = new LcdImageLine.Builder(32);
        LcdImageLine.Builder d = new LcdImageLine.Builder(64);
        
        assertEquals(32, b.size());
        assertEquals(64, d.size());
        
    }
    
    @Test
    public void setBytesWorksOnBigLine() {
        
        LcdImageLine.Builder b = new LcdImageLine.Builder(64);
        
//        assertEquals(bigColorLine_0(), b.build());
        
        b.setBytes(4, 0xF0, 0x0F);
        LcdImageLine l = b.build();
        
        assertEquals(bigLsb0FmsbF0().getMsb(), l.getMsb());
        assertEquals(bigLsb0FmsbF0().getLsb(), l.getLsb());
    }
    
    @Test
    public void setBytesFailsOnInvalidIndex() {
        LcdImageLine.Builder b = new LcdImageLine.Builder(32);
        
        assertThrows( IndexOutOfBoundsException.class,
                () -> b.setBytes(4, 0xf, 0xf));
        assertThrows( IndexOutOfBoundsException.class,
                () -> b.setBytes(-1, 0xf, 0xf));
    }
    
    @Test
    public void setBytesFailsOnInvalidBytes() {
        LcdImageLine.Builder b = new LcdImageLine.Builder(32);
        
        assertThrows( IllegalArgumentException.class,
                () -> b.setBytes(0, 0x100, 0xf));
        assertThrows( IllegalArgumentException.class,
                () -> b.setBytes(0, 0xf, 0x100));
    }
    
    // -------------------- GETTERS TEST     --------------------
    
    @Test
    public void sizeWorks() {
        assertEquals(32, colorLine_0().size());
        assertEquals(64, bigColorLine_0().size());
        
    }
    
    @Test
    public void msbWorks() {
        assertEquals(bit32_ff00(), colorLine_3_2_1_0().getMsb());
//        assertEquals(bit64_00f0_0000(), bigLsb0FmsbF0().msb());
    }
    
    @Test
    public void lsbWorks() {
        assertEquals(bit32_f0f0(), colorLine_3_2_1_0().getLsb());
//        assertEquals(bit64_000f_0000(), bigLsb0FmsbF0().lsb());
        
    }
    
//    @Test
//    public void opacityWorks() {
//        
//    }
    
    // -------------------- SHIFT TEST       --------------------
    @Test
    public void TrivialShiftRightWorks() {
        LcdImageLine l = line_2().shift(-1);
        assertEquals(singleBit1(), l.getMsb());
        assertEquals(singleBit1(), l.getLsb());
        assertEquals(singleBit1(), l.getOpacity());
    }
    
    @Test
    public void TrivialShiftLeftWorks() {
        LcdImageLine l = line_1().shift(1);
        assertEquals(singleBit2(), l.getMsb());
        assertEquals(singleBit2(), l.getLsb());
        assertEquals(singleBit2(), l.getOpacity());
    }
    
    @Test
    public void shiftDoesNothingForNullDelta() {
        assertEquals(colorLine_1_2_3_0(), colorLine_1_2_3_0().shift(0));
    }
    
    
    // -------------------- EXTRACT TEST     --------------------
    
    @Test
    public void trivialExtractWorks() {
        assertEquals(colorLine_0(), bigColorLine_0().extractWrapped(0, 32));
        LcdImageLine l = bigLsb0FmsbF0().extractWrapped(32, 32);
        assertEquals(lsb0FmsbF0(), l );
    }
    
    @Test
    public void extractWorksForBigSize() {
        assertEquals(big2Line1(), line_1().extractWrapped(0, 64));
    }
    
    @Test
    public void extractFailsForInvalidSize() {
        assertThrows(IllegalArgumentException.class, 
                () -> colorLine_0().extractWrapped(0, 33));
        assertThrows(IllegalArgumentException.class, 
                () -> colorLine_0().extractWrapped(0, 12));
        assertThrows(IllegalArgumentException.class, 
                () -> colorLine_0().extractWrapped(0, 0));
        assertThrows(IllegalArgumentException.class, 
                () -> colorLine_0().extractWrapped(0, -1));
                
    }
    
    @Test
    public void extractWorksOnNonTrivialValues() {
        LcdImageLine l = colorLine_1_2_3_0().extractWrapped(16, 32);
        assertEquals(colorLine_3_0_1_2(), l);
        
        
    }
    
    // -------------------- MAP COLORS TEST  --------------------
    
    @Test
    public void changeColorAllColors() {        
        LcdImageLine l = colorLine_3_2_1_0();
        LcdImageLine ex = colorLine_1_2_3_0();
        int palette = 0b00_11_10_01;
        LcdImageLine colored = l.mapColors(palette);
        
        assertEquals(colored.getMsb().toString(), ex.getMsb().toString());
        assertEquals(colored.getLsb().toString(), ex.getLsb().toString());   
    }
    
    @Test
    public void changeColorTrivial() {
        LcdImageLine l = colorLine_3_2_1_0();
        LcdImageLine ex = colorLine_3();
        int palette = 0b11_11_11_11;
        LcdImageLine colored = l.mapColors(palette);
        
        assertEquals(ex.getMsb().toString(), colored.getMsb().toString());
        assertEquals(ex.getLsb().toString(), colored.getLsb().toString());        
    }
    
    public void mapColorsFailsForinvalidPalette() {
        assertThrows(IllegalArgumentException.class,
                () -> colorLine_0().mapColors(0x100));
        assertThrows(IllegalArgumentException.class,
                () -> colorLine_0().mapColors(-1));
    }
    
    @Test
    public void mapColorsWorkForNonTrivialCase() {
        
    }
    
    
    // -------------------- BELOW TEST       --------------------
    
    @Test
    public void trivialBelowWorks() {
        assertEquals(colorLine_0(),
                colorLine_0().below(transparentLine()));
    }
    
    @Test
    public void belowFailsOnNonEqualLengths() {
        assertThrows(IllegalArgumentException.class, 
                () -> colorLine_0().below(big2Line1()));
    }
    
    @Test
    public void trivialBelowWorksGivenOpacity() {
        assertEquals(colorLine_3(),
                colorLine_0().below(transparentLine(), allOnes()));
    }
    
    @Test
    public void belowFailsOnWrongLengthOpacity() {
        assertThrows(IllegalArgumentException.class,
                () -> colorLine_0().below(transparentLine(), bit64_0()));
    }
     
    
    // -------------------- JOIN TEST        --------------------
    @Test
    public void joinWorksOnTrivialValues() {
        assertEquals(line_1(), colorLine_0().join(line_1(), 0));
    }
    
    @Test
    public void joinFailsOnDifferentLengths() {
        assertThrows(IllegalArgumentException.class,
                () -> colorLine_0().join(big2Line1(), 0));
        
    }
    
    @Test
    public void joinFailsOnInvalidIndex() {
        assertThrows(IllegalArgumentException.class,
                () -> colorLine_0().join(line_1(), -1));
        assertThrows(IllegalArgumentException.class,
                () -> colorLine_0().join(line_1(), 32));
    }
    
    @Test
    public void joinWorksForBigLines() {
        LcdImageLine l = bigLsb0FmsbF0().join(bigColorLine_0(), 32);

        assertEquals(bigColorLine_0(),l);
    }
    
    @Test
    public void joinWorksOnNonTrivialValues() {
        LcdImageLine l = colorLine_3_0_1_2().join(colorLine_0(), 24);
        System.out.println(l.getMsb());
      System.out.println(l.getLsb());
      System.out.println(l.getOpacity());
        assertEquals(colorLine_3_0_1_0(), l);
        
        
    }
    
    // -------------------- PIXEL COLOR TEST --------------------
    
    
    
    // ++++++++++++++++++++ PRESET LINES ++++++++++++++++++++++++
    
    
    private LcdImageLine colorLine_0() {
        int[] MSBchunks = { 0, 0, 0, 0};
        int[] LSBchunks = { 0, 0, 0, 0};
        int[] OPchunks =  {  0xff, 0xff, 0xff, 0xff};
        
        BitVector.Builder msbB = new Builder(Integer.SIZE);
        BitVector.Builder lsbB = new Builder(Integer.SIZE);
        BitVector.Builder opB =  new Builder(Integer.SIZE);
        for(int i = 0; i < Integer.BYTES; i++) {
            msbB.setByte(i, MSBchunks[i]);
            lsbB.setByte(i, LSBchunks[i]);
            opB.setByte(i, OPchunks[i]);
        }
        
        BitVector msb = msbB.build();
        BitVector lsb = lsbB.build();
        BitVector op = opB.build();
        
        return new LcdImageLine(msb, lsb, op);
    }
    
    private LcdImageLine colorLine_3() {
        int[] MSBchunks = { 0xff, 0xff, 0xff, 0xff};
        int[] LSBchunks = { 0xff, 0xff, 0xff, 0xff};
        int[] OPchunks =  { 0xff, 0xff, 0xff, 0xff};
        
        BitVector.Builder msbB = new Builder(Integer.SIZE);
        BitVector.Builder lsbB = new Builder(Integer.SIZE);
        BitVector.Builder opB =  new Builder(Integer.SIZE);
        for(int i = 0; i < Integer.BYTES; i++) {
            msbB.setByte(i, MSBchunks[i]);
            lsbB.setByte(i, LSBchunks[i]);
            opB.setByte(i, OPchunks[i]);
        }
        
        BitVector msb = msbB.build();
        BitVector lsb = lsbB.build();
        BitVector op = opB.build();
        
        return new LcdImageLine(msb, lsb, op);
    }

    private LcdImageLine colorLine_3_2_1_0() {
        int[] MSBchunks = { 0b0000_0000, 0b0000_0000,  0b1111_1111, 0b1111_1111};
        int[] LSBchunks = { 0b0000_0000, 0b1111_1111, 0b0000_0000, 0b1111_1111};
        int[] OPchunks = { 0xff, 0xff, 0xff, 0xff};
        
        BitVector.Builder msbB = new Builder(Integer.SIZE);
        BitVector.Builder lsbB = new Builder(Integer.SIZE);
        BitVector.Builder opB =  new Builder(Integer.SIZE);
        for(int i = 0; i < Integer.BYTES; i++) {
            msbB.setByte(i, MSBchunks[i]);
            lsbB.setByte(i, LSBchunks[i]);
            opB.setByte(i, OPchunks[i]);
        }
        
        BitVector msb = msbB.build();
        BitVector lsb = lsbB.build();
        BitVector op = opB.build();
        
        return new LcdImageLine(msb, lsb, op);
        
    }
    
    private LcdImageLine colorLine_1_2_3_0() {
        
        int[] MSBchunks = {  0b0000_0000, 0b1111_1111, 0b1111_1111, 0b0000_0000};
        int[] LSBchunks = {  0b1111_1111, 0b0000_0000, 0b1111_1111, 0b0000_0000};
        int[] OPchunks = { 0xff, 0xff, 0xff, 0xff};
        
        BitVector.Builder msbB = new Builder(Integer.SIZE);
        BitVector.Builder lsbB = new Builder(Integer.SIZE);
        BitVector.Builder opB =  new Builder(Integer.SIZE);
        for(int i = 0; i < Integer.BYTES; i++) {
            msbB.setByte(i, MSBchunks[i]);
            lsbB.setByte(i, LSBchunks[i]);
            opB.setByte(i, OPchunks[i]);
        }
        
        BitVector msb = msbB.build();
        BitVector lsb = lsbB.build();
        BitVector op = opB.build();
        
        return new LcdImageLine(msb, lsb, op);
    }
    
    private LcdImageLine colorLine_3_0_1_2() {
        
        int[] MSBchunks = { 0xff, 0, 0, 0xff,};
        int[] LSBchunks = { 0xff, 0, 0xff, 0,};
        int[] OPchunks = { 0xff, 0xff, 0xff, 0xff};
        
        BitVector.Builder msbB = new Builder(Integer.SIZE);
        BitVector.Builder lsbB = new Builder(Integer.SIZE);
        BitVector.Builder opB =  new Builder(Integer.SIZE);
        for(int i = 0; i < Integer.BYTES; i++) {
            msbB.setByte(i, MSBchunks[i]);
            lsbB.setByte(i, LSBchunks[i]);
            opB.setByte(i, OPchunks[i]);
        }
        
        BitVector msb = msbB.build();
        BitVector lsb = lsbB.build();
        BitVector op = opB.build();
        
        return new LcdImageLine(msb, lsb, op);
    }

    private LcdImageLine colorLine_3_0_1_0() {
        int[] MSBchunks = { 0xff, 0, 0, 0};
        int[] LSBchunks = { 0xff, 0, 0xff, 0,};
        int[] OPchunks = { 0xff, 0xff, 0xff, 0xff};
        
        BitVector.Builder msbB = new Builder(Integer.SIZE);
        BitVector.Builder lsbB = new Builder(Integer.SIZE);
        BitVector.Builder opB =  new Builder(Integer.SIZE);
        for(int i = 0; i < Integer.BYTES; i++) {
            msbB.setByte(i, MSBchunks[i]);
            lsbB.setByte(i, LSBchunks[i]);
            opB.setByte(i, OPchunks[i]);
        }
        
        BitVector msb = msbB.build();
        BitVector lsb = lsbB.build();
        BitVector op = opB.build();
        
        return new LcdImageLine(msb, lsb, op);
    }
    
    private LcdImageLine line_1() {
        
        return new LcdImageLine(singleBit1(), singleBit1(), singleBit1());
        
    }
    
    private LcdImageLine line_2() {
        
        return new LcdImageLine(singleBit2(), singleBit2(), singleBit2());
        
    }
    
    private LcdImageLine lsb0FmsbF0() {
            int[] MSBchunks = { 0xf0, 0, 0, 0};
            int[] LSBchunks = { 0x0f, 0, 0, 0};
            int[] OPchunks =  { 0xff, 0xff, 0xff, 0xff};
            
            BitVector.Builder msbB = new Builder(Integer.SIZE);
            BitVector.Builder lsbB = new Builder(Integer.SIZE);
            BitVector.Builder opB =  new Builder(Integer.SIZE);
            for(int i = 0; i < Integer.BYTES; i++) {
                msbB.setByte(i, MSBchunks[i]);
                lsbB.setByte(i, LSBchunks[i]);
                opB.setByte(i, OPchunks[i]);
            }
            
            BitVector msb = msbB.build();
            BitVector lsb = lsbB.build();
            BitVector op = opB.build();
            
            return new LcdImageLine(msb, lsb, op);
    }
    
    private LcdImageLine bigColorLine_0() {
        int[] MSBchunks = { 0, 0, 0, 0, 0, 0, 0, 0};
        int[] LSBchunks = { 0, 0, 0, 0, 0, 0, 0, 0};
        int[] OPchunks =  { 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,0xff, 0xff};
        
        BitVector.Builder msbB = new Builder(Integer.SIZE*2);
        BitVector.Builder lsbB = new Builder(Integer.SIZE*2);
        BitVector.Builder opB =  new Builder(Integer.SIZE*2);
        for(int i = 0; i < Integer.BYTES*2; i++) {
            msbB.setByte(i, MSBchunks[i]);
            lsbB.setByte(i, LSBchunks[i]);
            opB.setByte(i, OPchunks[i]);
        }
        
        BitVector msb = msbB.build();
        BitVector lsb = lsbB.build();
        BitVector op = opB.build();
        
        return new LcdImageLine(msb, lsb, op);
    }
    
    private LcdImageLine bigLsb0FmsbF0() {
        int[] MSBchunks = { 0, 0, 0, 0, 0xf0, 0, 0, 0};
        int[] LSBchunks = { 0, 0, 0, 0, 0x0f, 0, 0, 0};
        int[] OPchunks =  { 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,0xff, 0xff};
        
        BitVector.Builder msbB = new Builder(Integer.SIZE*2);
        BitVector.Builder lsbB = new Builder(Integer.SIZE*2);
        BitVector.Builder opB =  new Builder(Integer.SIZE*2);
        for(int i = 0; i < Integer.BYTES*2; i++) {
            msbB.setByte(i, MSBchunks[i]);
            lsbB.setByte(i, LSBchunks[i]);
            opB.setByte(i, OPchunks[i]);
        }
        
        BitVector msb = msbB.build();
        BitVector lsb = lsbB.build();
        BitVector op = opB.build();
        
        return new LcdImageLine(msb, lsb, op);
   }
    
    private LcdImageLine big2Line1() {
        
        return new LcdImageLine(bit64_10_10(), bit64_10_10(), bit64_10_10());
        
        
    }
    
    private LcdImageLine transparentLine() {
        return new LcdImageLine(allOnes(), allOnes(), allZeros());
    }
    
    private LcdImageLine emptyLine() {
        return new LcdImageLine(allZeros(), allZeros(), allZeros());
    }
    
    private BitVector allOnes() {
        int[] chunks = { 0xff, 0xff, 0xff, 0xff};
        
        BitVector.Builder b = new Builder(Integer.SIZE);
        for(int i = 0; i < Integer.BYTES; i++) {
            b.setByte(i, chunks[i]);
        }
        
        return b.build();
    }
    
    private BitVector allZeros() {
        int[] chunks = { 0, 0, 0, 0};
        
        BitVector.Builder b = new Builder(Integer.SIZE);
        for(int i = 0; i < Integer.BYTES; i++) {
            b.setByte(i, chunks[i]);
        }
        
        return b.build();
    }
    
    private BitVector singleBit1() {
        int[] chunks = { 1, 0, 0, 0};
        
        BitVector.Builder b = new Builder(Integer.SIZE);
        for(int i = 0; i < Integer.BYTES; i++) {
            b.setByte(i, chunks[i]);
        }
        
        return b.build();
    }
    
    private BitVector singleBit32() {
        int[] chunks = { 0, 0, 0, 0x80};
        
        BitVector.Builder b = new Builder(Integer.SIZE);
        for(int i = 0; i < Integer.BYTES; i++) {
            b.setByte(i, chunks[i]);
        }
        
        return b.build();
    }
    
    private BitVector singleBit2() {
        int[] chunks = { 0x02, 0, 0, 0};
        
        BitVector.Builder b = new Builder(Integer.SIZE);
        for(int i = 0; i < Integer.BYTES; i++) {
            b.setByte(i, chunks[i]);
        }
        
        return b.build();
    }
    
    private BitVector bit32_ff00() {
        int[] chunks = { 0, 0, 0xff, 0xff };
        
        BitVector.Builder b = new BitVector.Builder(Integer.SIZE);
        for(int i = 0; i < Integer.BYTES; i++) {
            b.setByte(i, chunks[i]);
        }
        
        return b.build();
        
    }
    
    private BitVector bit32_0f0f() {
        int[] chunks = { 0xff, 0, 0xff, 0};
        
        BitVector.Builder b = new BitVector.Builder(Integer.SIZE);
        for(int i = 0; i < Integer.BYTES; i++) {
            b.setByte(i, chunks[i]);
        }
        
        return b.build();
        
    }
    
    private BitVector bit32_f0f0() {
        int[] chunks = {  0, 0xff, 0, 0xff};
        
        BitVector.Builder b = new BitVector.Builder(Integer.SIZE);
        for(int i = 0; i < Integer.BYTES; i++) {
            b.setByte(i, chunks[i]);
        }
        
        return b.build();
        
    }
    
    private BitVector bit64_00f0_0000() {
        int[] chunks = {0, 0, 0, 0, 0, 0x0f, 0, 0 };
        
        BitVector.Builder b = new BitVector.Builder(Integer.SIZE*2);
        for(int i = 0; i < Integer.BYTES * 2; i++) {
            b.setByte(i, chunks[i]);
        }
        
        return b.build();
        
    }
    
    private BitVector bit64_000f_0000() {
        int[] chunks = {0, 0, 0, 0, 0, 0xf, 0, 0 };
        
        BitVector.Builder b = new BitVector.Builder(Integer.SIZE*2);
        for(int i = 0; i < Integer.BYTES * 2; i++) {
            b.setByte(i, chunks[i]);
        }
        
        return b.build();
        
    }
    
    private BitVector bit64_0() {
        int[] chunks = { 0, 0, 0, 0, 0, 0, 0, 0};
        
        BitVector.Builder b = new Builder(Integer.SIZE*2);
        for(int i = 0; i < Integer.BYTES * 2; i++) {
            b.setByte(i, chunks[i]);
        }
        
        return b.build();
    }
    
    private BitVector bit64_1() {
        int[] chunks = {  0xff, 0xff, 0xff, 0xff,
                          0xff, 0xff, 0xff, 0xff};
        
        BitVector.Builder b = new Builder(Integer.SIZE*2);
        for(int i = 0; i < Integer.BYTES * 2; i++) {
            b.setByte(i, chunks[i]);
        }
        
        return b.build();
    }
    
    private BitVector bit64_10_10() {
        int[] chunks = {  1, 0, 0, 0,
                          1, 0, 0, 0};

        BitVector.Builder b = new Builder(Integer.SIZE*2);
        for(int i = 0; i < Integer.BYTES * 2; i++) {
          b.setByte(i, chunks[i]);
        }
        
        return b.build();
    }
    
    
}
