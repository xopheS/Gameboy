// Gameboj stage 1

package ch.epfl.test;

import java.util.Random;

public interface TestRandomizer {
    // Fix random seed to guarantee reproducibility.
    long SEED = 2018;

    int RANDOM_ITERATIONS = 100;

    static Random newRandom() {
        return new Random(SEED);
    }
}
