package com.styliste.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    @NotEmpty(message = "Order items cannot be empty")
    private List<CartItemDTO> items;

    @NotBlank(message = "Shipping address cannot be blank")
    private String shippingAddress;
}
