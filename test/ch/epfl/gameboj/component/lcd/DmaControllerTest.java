package ch.epfl.gameboj.component.lcd;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.Bus;

class DmaControllerTest {
    @BeforeEach
    void setupBeforeEach() {

    }

    @Test
    void testSetBus() {
        fail("Not yet implemented");
    }

    @Test
    void testStart() {
        fail("Not yet implemented");
    }

    @Test
    void testCopy() {
        fail("Not yet implemented");
    }

    @Test
    void testIsActive() {
        DmaController dmaController = DmaController.getDmaController();
        dmaController.setBus(new Bus());
        dmaController.start(0x10);

        assertTrue(dmaController.isActive());
    }

}
