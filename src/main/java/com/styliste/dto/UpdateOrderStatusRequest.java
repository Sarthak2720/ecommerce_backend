package com.styliste.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStatusRequest {

    @NotBlank(message = "Status cannot be blank")
    private String status;

    private String trackingNumber;
    private String timelineMessage; // 👈 Allow admin to type: "Packed and ready to go!"
}
