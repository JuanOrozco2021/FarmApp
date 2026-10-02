package com.boticario.modules.catalog.api.dto;

import com.boticario.modules.catalog.domain.PriceHistory;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceHistoryDTO(BigDecimal oldPrice, BigDecimal newPrice, String changedBy, Instant changedAt, String reason) {
    public static PriceHistoryDTO from(PriceHistory history) {
        return new PriceHistoryDTO(history.getOldPrice(), history.getNewPrice(), history.getChangedBy(), history.getChangedAt(), history.getReason());
    }
}
