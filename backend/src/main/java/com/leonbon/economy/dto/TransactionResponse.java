package com.leonbon.economy.dto;

import com.leonbon.economy.TransactionType;
import java.time.Instant;

public record TransactionResponse(
        String id,
        TransactionType type,
        long amount,
        long balanceAfter,
        String ref,
        Instant createdAt
) {}

