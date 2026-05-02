package com.leonbon.tournaments;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BracketMathTest {

    @Test
    void nextPow2_sizes() {
        assertEquals(2, BracketMath.nextPow2(1));
        assertEquals(2, BracketMath.nextPow2(2));
        assertEquals(4, BracketMath.nextPow2(3));
        assertEquals(8, BracketMath.nextPow2(5));
        assertEquals(8, BracketMath.nextPow2(8));
    }

    @Test
    void roundsForBracketSize() {
        assertEquals(1, BracketMath.roundsForBracketSize(2));
        assertEquals(2, BracketMath.roundsForBracketSize(4));
        assertEquals(3, BracketMath.roundsForBracketSize(8));
    }
}
