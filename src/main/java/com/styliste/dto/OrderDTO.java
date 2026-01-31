package com.styliste.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String status;
    private String paymentStatus;
    private BigDecimal totalAmount;
    private BigDecimal discount;
    private BigDecimal tax;
    private String trackingNumber;
    private String shippingAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemDTO> items;
    private List<OrderTimelineDTO> timeline; // 👈 Array for the UI Stepper
}
