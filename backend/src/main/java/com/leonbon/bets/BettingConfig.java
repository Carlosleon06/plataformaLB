package com.leonbon.bets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BettingConfig {
    /** Minutos después de que el admin abre apuestas hasta el cierre automático. */
    private final int windowMinutes;
    private final int slotStaggerMinutes;

    public BettingConfig(
            @Value("${app.bets.windowMinutes:5}") int windowMinutes,
            @Value("${app.bets.slotStaggerMinutes:45}") int slotStaggerMinutes
    ) {
        this.windowMinutes = windowMinutes;
        this.slotStaggerMinutes = slotStaggerMinutes;
    }

    public int windowMinutes() {
        return windowMinutes;
    }

    public int slotStaggerMinutes() {
        return slotStaggerMinutes;
    }
}
