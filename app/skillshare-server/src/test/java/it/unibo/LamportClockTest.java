package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LamportClockTest {

    private LamportClock clock;

    @BeforeEach
    void setUp() {
        DatabaseCore.enableTestMode();
        clock = new LamportClock();
    }

    @AfterEach
    void tearDown() {
        DatabaseCore.disableTestMode();
    }

    @Test
    void startsAtZero() {
        assertEquals(0, clock.current());
    }

    @Test
    void tickIncrementsByOne() {
        assertEquals(1, clock.tick());
        assertEquals(2, clock.tick());
        assertEquals(3, clock.tick());
        assertEquals(3, clock.current());
    }

    @Test
    void updateTakesMaxPlusOne() {
        clock.tick(); // 1
        clock.tick(); // 2

        // received is ahead -> jump past it
        assertEquals(11, clock.update(10));
        // received is behind -> just advance locally
        assertEquals(12, clock.update(5));
    }

    @Test
    void currentDoesNotAdvance() {
        clock.tick();
        assertEquals(1, clock.current());
        assertEquals(1, clock.current());
    }

    @Test
    void valueIsSharedAndPersistedAcrossInstances() {
        clock.tick();
        clock.tick(); // value = 2

        LamportClock another = new LamportClock();
        assertEquals(2, another.current());
        assertEquals(3, another.tick());
        // the original instance sees the shared advance too
        assertEquals(3, clock.current());
    }
}