package ch.epfl.gameboj.component.lcd;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;

class DmaControllerTest {
    DmaController testController;
    Bus mockBus;
    
    @BeforeEach
    void setupBeforeEach() {
        mockBus = mock(Bus.class);
        testController = DmaController.getDmaController();
    }

    @Test
    void testSetBusThrowsIllegalStateExceptionWhenBusNotSet() {
        assertThrows(IllegalStateException.class, () -> testController.start(0x50));
    }

    @Disabled
    @Test
    void testStart() {
        fail("Not yet implemented");
    }

    @Test
    void testCopy() {
        testController.setBus(mockBus);
        
        for (int i = 0; i < 200; ++i) {
            when(mockBus.read(0b00000011_00000000 + i)).thenReturn(i % 50);
            when(mockBus.read(AddressMap.OAM_START + i)).thenReturn(i % 50);
        }
        
        testController.start(0b00000011);
        
        for (int i = 0; i < 160; ++i) {
            testController.copy();
        }
        
        verify(mockBus, times(160)).read(anyInt());
        
        for (int i = 0; i < 160; ++i) {
            System.out.println(i);
            assertThat(mockBus.read(AddressMap.OAM_START + i), is(equalTo(i % 50)));
        }
    }

    @Test
    void testIsActive() {
        DmaController dmaController = DmaController.getDmaController();
        dmaController.setBus(new Bus());
        dmaController.start(0x10);

        assertTrue(dmaController.isActive());
    }
}
