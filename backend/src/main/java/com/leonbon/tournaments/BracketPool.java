package com.leonbon.tournaments;

/**
 * Which sub-bracket a match belongs to. Single-elimination uses {@link #WB} only.
 * Double-elimination uses {@link #WB}, {@link #LB}, and {@link #GF}. Round-robin uses {@link #RR}.
 */
public enum BracketPool {
    WB,
    LB,
    GF,
    RR
}
