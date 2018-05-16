package ch.epfl.gameboj.bits;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class BitVectorExtraTest {

    BitVector v1, v2, v3;

    @BeforeEach
    void initAll() {
        // 00000000_11111111_00000000_00000000_11001100_00000000_10101010_11110000
        v1 = new BitVector.Builder(64).setByte(0, 0b1111_0000).setByte(1 * Byte.SIZE, 0b1010_1010)
                .setByte(3 * Byte.SIZE, 0b1100_1100).setByte(6 * Byte.SIZE, 0b1111_1111).build();

        // 00000000_00110110_00000000_00000000_11011110_00000000_10001000_11011100
        v2 = new BitVector.Builder(64).setByte(0, 0b1101_1100).setByte(1 * Byte.SIZE, 0b1000_1000)
                .setByte(3 * Byte.SIZE, 0b1101_1110).setByte(6 * Byte.SIZE, 0b0011_0110).build();

        // 0000000000110110000000000000000011011110000000001000100011011100
        v3 = new BitVector.Builder(64).setByte(0, 0b1101_1100).setByte(1 * Byte.SIZE, 0b1000_1000)
                .setByte(3 * Byte.SIZE, 0b1101_1110).setByte(6 * Byte.SIZE, 0b0011_0110).build();
    }

    @Test
    void shiftWorks() {
        assertThat(v1.shift(-10).toString(),
                is(equalTo("0000000000000000001111111100000000000000001100110000000000101010")));
    }

    @Test
    void testMaskMSB() {
        assertThat(v1.maskMSB(10).toString(),
                is(equalTo("1111111111111111111111111111111111111111111111111111110000000000")));
    }

    @Test
    void testMaskLSB() {
        assertThat(v1.maskLSB(10).not().toString(),
                is(equalTo("1111111111111111111111111111111111111111111111111111110000000000")));
    }

    @Disabled
    @Test
    void testMask() {
        fail("Not yet implemented");
    }

    @Test
    void testClipMSB() {
        assertThat(v1.clipMSB(10).toString(),
                is(equalTo("0000000011111111000000000000000011001100000000001010100000000000")));
    }

    @Test
    void testClipLSB() {
        assertThat(v1.clipLSB(10).toString(),
                is(equalTo("0000000000000000000000000000000000000000000000000000001011110000")));
    }

}
