package ch.epfl.gameboj.component.memory;

import static ch.epfl.gameboj.component.memory.VideoRamController.BYTES_PER_TILE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.AddressMap;

class VideoRamControllerTest {
    static Random randomGen;
    
    VideoRamController testController;

    @BeforeAll
    static void setupBeforeAll() {
        randomGen = new Random();
    }
    
    @BeforeEach
    void setupBeforeEach() {
        Ram ram = new Ram(AddressMap.VRAM_SIZE);
        testController = new VideoRamController(ram, AddressMap.VRAM_START);
    }

    @Test
    void testTileLineBytesIntIntBooleanBoolean() {
        int tileTypeIndex = 3;
        int tileLineIndex = 5;
        int data = 0x50;
        boolean tileSource = true;
        System.out.println("writing at " + AddressMap.TILE_SOURCE[tileSource ? 1 : 0] + tileTypeIndex * BYTES_PER_TILE + tileLineIndex);
        testController.write(AddressMap.TILE_SOURCE[tileSource ? 1 : 0] + tileTypeIndex * BYTES_PER_TILE + 2 * tileLineIndex, data);
        
        assertThat(testController.tileLineBytes(3, 5, tileSource, false), is(equalTo(data)));
    }

    @Test
    void testTileLineBytesIntIntBooleanIntIntBoolean() {
        fail("Not yet implemented");
    }

    @RepeatedTest(5)
    void testTileByteAddress() {
        int tileTypeIndex = randomGen.nextInt(256);
        int tileLineIndex = randomGen.nextInt(8);
        boolean tileSource = randomGen.nextBoolean();
        int expectedAddress;
        
        if (tileSource) {
            expectedAddress = AddressMap.TILE_SOURCE[1] + tileTypeIndex * BYTES_PER_TILE + 2 * tileLineIndex;
        } else {
            if (tileTypeIndex < 128) {
                expectedAddress = AddressMap.TILE_SOURCE[0] + (tileTypeIndex + 128) * BYTES_PER_TILE + 2 * tileLineIndex;
            } else {
                expectedAddress = AddressMap.TILE_SOURCE[0] + (tileTypeIndex - 128) * BYTES_PER_TILE + 2 * tileLineIndex;
            }
        }
        
        assertThat(testController.tileByteAddress(tileTypeIndex, tileLineIndex, tileSource), is(equalTo(expectedAddress)));
    }
}
