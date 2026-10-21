package edu.arch.shop.order.dto;

import java.math.BigDecimal;

public record OrderRequest(
        String customerName,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
) {
}
