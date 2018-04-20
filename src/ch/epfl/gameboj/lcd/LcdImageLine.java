package ch.epfl.gameboj.lcd;

import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;

import java.util.Arrays;
import java.util.Objects;

import ch.epfl.gameboj.Preconditions;

public class LcdImageLine {
      //must be immuable
    
    
    private final BitVector lsb;
    private final BitVector msb;
    private final BitVector opacity;
    
    public LcdImageLine(BitVector lsb, BitVector msb, BitVector opacity) {
        
        Preconditions.checkArgument(lsb.size() == msb.size() && msb.size() == lsb.size());
        this.lsb = lsb;
        this.msb = msb;
        this.opacity = opacity;
   
    }
    
    public int size() {
        return lsb.size();
    }
    
    public BitVector getLsb() {
        return this.lsb;
    }
    
    public BitVector getMsb() {
        return this.msb;
    }
    
    public BitVector getOpacity() {
        return this.opacity;
    }
    
    public LcdImageLine shift(int distance) {
        return new LcdImageLine(lsb.shift(distance), msb.shift(distance), opacity.shift(distance));
    }
    
    public LcdImageLine extractWrapped(int pixel, int size) {
        return new LcdImageLine(lsb.extractWrapped(pixel, size), msb.extractWrapped(pixel, size), opacity.extractWrapped(pixel, size));
    }
    
    
    public LcdImageLine mapColors(int palette) {
        Preconditions.checkBits8(palette);
        
        BitVector msbCopy = msb.extractZeroExtended(0, size()), lsbCopy = lsb.extractZeroExtended(0, size());
        
        if(palette == 0b11100100) return this;
        
        int[] paletteMSB = new int[4], paletteLSB = new int[4];
        
        for(int i = 0; i < 4; i++) {
            paletteMSB[i] = Bits.test(palette, 2*i + 1) ? 1 : 0;
            paletteLSB[i] = Bits.test(palette, 2*i) ? 1 : 0; 
        }
        
        if(paletteMSB[0] == 1) {
            msbCopy = msbCopy.or(msbCopy.not().and(lsbCopy.not()));
        }
        if(paletteMSB[1] == 1) {
            msbCopy = msbCopy.or(msbCopy.not().and(lsbCopy));
        }
        if(paletteMSB[2] == 1) {
            msbCopy = msbCopy.or(msbCopy.and(lsbCopy.not()));
        }
        if(paletteMSB[3] == 1) {
            msbCopy = msbCopy.or(msbCopy.and(lsbCopy));
        }
        
        if(paletteLSB[0] == 1) {
            lsbCopy = lsbCopy.or(msbCopy.not().and(lsbCopy.not()));
        }
        if(paletteLSB[1] == 1) {
            lsbCopy = lsbCopy.or(msbCopy.not().and(lsbCopy));
        }
        if(paletteLSB[2] == 1) {
            lsbCopy = lsbCopy.or(msbCopy.and(lsbCopy.not()));
        }
        if(paletteLSB[3] == 1) {
            lsbCopy = lsbCopy.or(msbCopy.and(lsbCopy));
        }
        
        return new LcdImageLine(msbCopy, lsbCopy, opacity);
    
    }
    
    public LcdImageLine below(LcdImageLine other) {
        
        int lcdLineSize = size();
        BitVector modifiedLSB = new BitVector(lcdLineSize), modifiedMSB = new BitVector(lcdLineSize);
        
        for(int i = 0; i < lcdLineSize; i++) {
            
            int newColor = !other.getOpacity().testBit(i) ? (this.getLsb().getArray()[i] + 2*this.getMsb().getArray()[i]) : (other.getLsb().getArray()[i] + 2*other.getMsb().getArray()[i]);     
                 
        }
        
        return new LcdImageLine(, , );
    }
    
    
    public LcdImageLine below(LcdImageLine other, BitVector opacity) {
        
        int lcdLineSize = size();
        BitVector.Builder msbBuilder = new BitVector.Builder(lcdLineSize);
        BitVector.Builder lsbBuilder = new BitVector.Builder(lcdLineSize);
        
        for(int i = 0; i < lcdLineSize; i++) {
            
            if(opacity.testBit(i)) {
                
                int newColor0 = other.getLsb().getArray()[i] + 2*other.getMsb().getArray()[i];
                int newColor1 = other.getLsb().getArray()[i+1] + 2*other.getMsb().getArray()[i+1];
                int newColor2 = other.getLsb().getArray()[i+2] + 2*other.getMsb().getArray()[i+2];
                int newColor3 = other.getLsb().getArray()[i+3] + 2*other.getMsb().getArray()[i+3];
            }
            
            else {
                int newColor0 = this.getLsb().getArray()[i] + 2*this.getMsb().getArray()[i];
                int newColor1 = this.getLsb().getArray()[i+1] + 2*this.getMsb().getArray()[i+1];
                int newColor2 = this.getLsb().getArray()[i] + 2*this.getMsb().getArray()[i];
                int newColor3 = this.getLsb().getArray()[i] + 2*this.getMsb().getArray()[i];
            }
           
            
            lsbBuilder.setByte(i, newColor & 0b1);
            msbBuilder.setByte(i, (newColor & 0b10) >>> 1);     
        }
        
        return new LcdImageLine(lsbBuilder.build(), msbBuilder.build(), this.opacity);
    }
    
    public LcdImageLine join(LcdImageLine other, int n) {
        
        int lcdLineSize = size();
        BitVector.Builder msbBuilder = new BitVector.Builder(lcdLineSize);
        BitVector.Builder lsbBuilder = new BitVector.Builder(lcdLineSize);
        
        for(int i = 0; i < lcdLineSize; i++) {
            
            
            lsbBuilder.setByte(i, newColor & 0b1);
            msbBuilder.setByte(i, (newColor & 0b10) >>> 1);     
        }
        
        return new LcdImageLine(lsbBuilder.build(), msbBuilder.build(), opacity);
        
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof LcdImageLine) && lsb.equals(((LcdImageLine)o).getLsb()) && msb.equals(((LcdImageLine)o).getMsb()) && opacity.equals(((LcdImageLine)o).getOpacity());
    }
    
    @Override    
    public int hashCode() {
        return Objects.hash(msb, lsb, opacity);
    }
    
    
    public final static class Builder {
       
        BitVector.Builder msbBuilder;
        BitVector.Builder lsbBuilder;
        
        public Builder(int size) {
            msbBuilder = new BitVector.Builder(size);
            lsbBuilder = new BitVector.Builder(size);
        }

        public Builder setBytes(int index, int b) {          
            msbBuilder.setByte(index, b);
            lsbBuilder.setByte(index, b);
            return this;
        }
        
      
        
    }
    
}
