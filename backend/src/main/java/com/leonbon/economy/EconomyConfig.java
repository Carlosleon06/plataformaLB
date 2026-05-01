package com.leonbon.economy;

import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EconomyConfig {
    private final long dailyClaimAmount;
    private final ZoneId zoneId;
    private final int dailyResetHour;

    public EconomyConfig(
            @Value("${app.economy.dailyClaimAmount}") long dailyClaimAmount,
            @Value("${app.economy.timezone}") String timezone,
            @Value("${app.economy.dailyResetHour}") int dailyResetHour
    ) {
        this.dailyClaimAmount = dailyClaimAmount;
        this.zoneId = ZoneId.of(timezone);
        this.dailyResetHour = dailyResetHour;
    }

    public long dailyClaimAmount() {
        return dailyClaimAmount;
    }

    public ZoneId zoneId() {
        return zoneId;
    }

    public int dailyResetHour() {
        return dailyResetHour;
    }
}

