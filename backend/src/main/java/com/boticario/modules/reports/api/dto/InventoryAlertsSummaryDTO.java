package com.boticario.modules.reports.api.dto;

public record InventoryAlertsSummaryDTO(
        int lowStockCount,
        int expiringSoonCount,
        int expiredCount
) {
}
