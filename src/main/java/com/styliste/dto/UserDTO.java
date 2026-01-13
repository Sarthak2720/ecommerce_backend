package com.styliste.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String role;     // String is easier for frontend than Enum
    private Boolean isActive;
    private LocalDateTime createdAt;

    // Optional: If you want to show how many orders/appointments they have without loading the full list
    private int orderCount;
    private int appointmentCount;
}