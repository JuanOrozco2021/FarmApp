package com.boticario.modules.sales.api.dto;

import com.boticario.modules.sales.domain.SaleItemLot;

import java.math.BigDecimal;

public record SaleItemLotInfo(String lotCode, BigDecimal quantityBase) {
    public static SaleItemLotInfo from(SaleItemLot lot) {
        return new SaleItemLotInfo(lot.getLot().getLotCode(), lot.getQuantityBase());
    }
}
