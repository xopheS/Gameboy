// Gameboj stage 1

package ch.epfl.gameboj.component;

import java.util.Random;

import static ch.epfl.test.TestRandomizer.newRandom;
import static ch.epfl.test.TestRandomizer.RANDOM_ITERATIONS;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public interface ComponentTest {
    Component newComponent();

    @Test
    default void readFailsForInvalidAddress() {
        Random rng = newRandom();
        Component c = newComponent();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int a0 = rng.nextInt();
            while (0 <= a0 && a0 <= 0xFFFF) {
                a0 += 0xFFFF;
            }
            int a = a0;
            assertThrows(IllegalArgumentException.class,
                () -> c.read(a));
        }
    }
    
    @Test
    default void writeFailsForInvalidAddress() {
        Random rng = newRandom();
        Component c = newComponent();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int a0 = rng.nextInt();
            while (0 <= a0 && a0 <= 0xFFFF) {
                a0 += 0xFFFF;
            }
            int a = a0;
            assertThrows(IllegalArgumentException.class,
                () -> c.write(a, 0));
        }
    }
    
    @Test
    default void writeFailsForInvalidData() {
        Random rng = newRandom();
        Component c = newComponent();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int d0 = rng.nextInt();
            while (0 <= d0 && d0 <= 0xFF) {
                d0 += 0xFF;
            }
            int d = d0;
            assertThrows(IllegalArgumentException.class,
                () -> c.write(0, d));
        }
    }
}
