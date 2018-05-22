package ch.epfl.gameboj.component.memory;

import static ch.epfl.gameboj.component.memory.OamRamController.SPRITE_ATTR_BYTES;
import static ch.epfl.gameboj.component.memory.OamRamController.MAX_SPRITES;
import static ch.epfl.gameboj.component.memory.OamRamController.SPRITE_XOFFSET;
import static ch.epfl.gameboj.component.memory.OamRamController.SPRITE_YOFFSET;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import org.mockito.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.memory.OamRamController.ATTRIBUTES;
import ch.epfl.gameboj.component.memory.OamRamController.DISPLAY_DATA;

class OamRamControllerTest {
    static Random randomGen;
    
    OamRamController testController;

    @BeforeAll
    static void setupBeforeAll() {
        randomGen = new Random();
    }
    
    @BeforeEach
    void setupBeforeEach() {
        Ram ram = new Ram(AddressMap.OAM_SIZE);
        RamController controller = new RamController(ram, AddressMap.OAM_START);
        testController = new OamRamController(controller);
    }
    
    @Test
    void testOamRamController() {
        
    }

    @RepeatedTest(5)
    void testSpritesIntersectingLine() {
        int sIndex1 = randomGen.nextInt(MAX_SPRITES), sIndex2 = randomGen.nextInt(MAX_SPRITES), sIndex3 = randomGen.nextInt(MAX_SPRITES);
        placeSprite(sIndex1, 30, 50);
        placeSprite(sIndex2, 30, 52);
        placeSprite(sIndex3, 30, 51);
        
        int[] results = new int[] { 
                testController.packSpriteInfo(sIndex1), 
                testController.packSpriteInfo(sIndex2),
                testController.packSpriteInfo(sIndex3) };
        
        Arrays.sort(results);
        
        assertThat(testController.spritesIntersectingLine(55, 8), is(equalTo(results)));
    }
    
    private void placeSprite(int spriteIndex, int spriteX, int spriteY) {
        testController.write(AddressMap.OAM_START + spriteIndex * SPRITE_ATTR_BYTES + DISPLAY_DATA.X_COORD.ordinal(), spriteX + SPRITE_XOFFSET);
        testController.write(AddressMap.OAM_START + spriteIndex * SPRITE_ATTR_BYTES + DISPLAY_DATA.Y_COORD.ordinal(), spriteY + SPRITE_YOFFSET);
    }

    @RepeatedTest(5)
    void testReadAttr() {
        int spriteIndex = randomGen.nextInt(20);
        testController.write(AddressMap.OAM_START + spriteIndex * SPRITE_ATTR_BYTES + DISPLAY_DATA.ATTRIBUTES.ordinal(), Bits.mask(ATTRIBUTES.FLIP_H.index()));
        
        assertThat(testController.readAttr(spriteIndex, DISPLAY_DATA.ATTRIBUTES), is(equalTo(Bits.mask(ATTRIBUTES.FLIP_H.index()))));
    }
}
