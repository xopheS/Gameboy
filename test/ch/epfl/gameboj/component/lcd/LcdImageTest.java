package ch.epfl.gameboj.component.lcd;

import static org.junit.jupiter.api.Assertions.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LcdImageTest {
    LcdImage lcdImage;
    
    @BeforeEach
    void setupBeforeEach() {
        
    }

    @Test
    void testLcdImage() {
        fail("Not yet implemented");
    }

    @Test
    void testGetWidth() {
        lcdImage = new LcdImage(120, 340, new ArrayList<LcdImageLine>(10));
        assertThat(lcdImage.getWidth(), is(equalTo(120)));
    }

    @Test
    void testGetHeight() {
        lcdImage = new LcdImage(120, 340, new ArrayList<LcdImageLine>(10));
        assertThat(lcdImage.getHeight(), is(equalTo(340)));
    }

    @Test
    void testGetPixel() {
        fail("Not yet implemented");
    }

    @Test
    void testEqualsObjectWorksWhenStructurallyEqual() {
        fail("Not yet implemented");
    }
    
    @Test
    void testEqualsObjectFailsWhenStructurallyDifferent() {
        fail("Not yet implemented");
    }
}
