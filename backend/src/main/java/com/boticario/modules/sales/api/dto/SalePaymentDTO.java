package com.boticario.modules.sales.api.dto;

import com.boticario.modules.sales.domain.SalePayment;

import java.math.BigDecimal;

public record SalePaymentDTO(String paymentMethod, BigDecimal amount) {
    public static SalePaymentDTO from(SalePayment payment) {
        return new SalePaymentDTO(payment.getPaymentMethod().name(), payment.getAmount());
    }
}
