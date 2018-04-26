package ch.epfl.gameboj.component.lcd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.BitVector;


public class LcdImage {
    //must be immuable
    
    private final int width;
    private final int height;
    private final List<LcdImageLine> imageLines;
    
    public LcdImage(int width, int height, List<LcdImageLine> list) {
       this.width = width;
       this.height = height;
       imageLines = Collections.unmodifiableList(new ArrayList<>(list));   
    }
    
    public int getWidth() {
        return this.width;
    }
    
    public int getHeight() {
        return this.height;
    }
    
    public int get(int x, int y) {
        Preconditions.checkArgument(x < width && x >= 0 && y < height && y >= 0, "Pixel coordinates must be within the bounds of the screen");
        int lsb = imageLines.get(y).getLsb().testBit(x) ? 1 : 0;
        int msb = (imageLines.get(y).getMsb().testBit(x) ? 1 : 0) << 1;
        return (lsb | msb);
    }
    
    @Override
    public boolean equals(Object o) {
    	for(int i = 0; i < height; ++i) {
    		if(!imageLines.get(i).equals(((LcdImage) o).imageLines.get(i))) return false;
    	}
    	
        return (o instanceof LcdImage)
                && height == (((LcdImage) o).height)
                && width == (((LcdImage) o).width);
    }

    @Override
    public int hashCode() {
        return Objects.hash(height, width, imageLines);
    }
       
    public static final class Builder {
        int height;
        int width;
        List<LcdImageLine> imageLines;
        
        public Builder(int width, int height) {
            this.width = width;
            this.height = height;
            imageLines = new ArrayList<>();
            for (int i = 0; i < height; i++) imageLines.add(new LcdImageLine(new BitVector(width), new BitVector(width), new BitVector(width))); 
        }
        
        public Builder setLine(int index, LcdImageLine l) {
            Preconditions.checkArgument(index < height);
            imageLines.set(index, l);
            return this;
        }
        
        public LcdImage build() {
            return new LcdImage(width, height, imageLines);
        } 
    }
}
