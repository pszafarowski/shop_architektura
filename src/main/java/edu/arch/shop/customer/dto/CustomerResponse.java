package edu.arch.shop.customer.dto;

public record CustomerResponse(
        Long id,
        String first_name,
        String last_name
) {
}
