package com.styliste.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    @NotEmpty(message = "Order items cannot be empty")
    private List<CartItemDTO> items;

    // Use a nested object instead of a single string
    @NotNull(message = "Shipping details are required")
    private AddressDTO shippingAddress;
}