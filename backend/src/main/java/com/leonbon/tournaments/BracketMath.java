package com.leonbon.tournaments;

/** Pure bracket sizing helpers (single / double elimination winners bracket). */
public final class BracketMath {
    private BracketMath() {}

    public static int nextPow2(int n) {
        if (n <= 1) {
            return 2;
        }
        int p = 1;
        while (p < n) {
            p <<= 1;
        }
        return p;
    }

    /** Number of rounds for a single-elimination bracket of size {@code m} (power of two, m >= 2). */
    public static int roundsForBracketSize(int m) {
        int r = 0;
        for (int x = m; x > 1; x >>= 1) {
            r++;
        }
        return r;
    }
}
