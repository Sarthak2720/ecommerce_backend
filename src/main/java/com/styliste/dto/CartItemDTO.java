package com.styliste.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemDTO {
    private Long id;            // CartItem ID (null for guest items)
    private Long productId;     // Required for merging
    private String productName;  // For UI display
    private String productImage; // First image URL for UI
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private boolean isSoldOut;         // 👈 Flag for 0 stock
    private String selectedSize;
    private String selectedColor;
    private boolean isQuantityAdjusted; // 👈 Add this
}