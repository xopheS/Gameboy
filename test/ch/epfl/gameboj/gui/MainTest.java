package ch.epfl.gameboj.gui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

class MainTest {
    
    @BeforeEach
    void setupBeforeEach() {
        Main mainUnderTest = new Main();
    }

    @Test
    void testMainLaunchesApplication() {
        //Main.launch("xxx");
    }

    @Test
    void testStartStage() {
        fail("Not yet implemented");
    }
}
