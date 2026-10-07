package com.akshat.SpringEcom.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record OrderRequest(
        @NotBlank String customerName,
        @NotBlank @Email String email,
        List<OrderItemRequest> items
) {
}