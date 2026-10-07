package com.akshat.SpringEcom.model.dto;

public record OrderItemRequest(
        int productId,
        int quantity
) {
}
