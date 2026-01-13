package com.styliste.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAppointmentRequest {
    private String status;
    private String notes;
}
