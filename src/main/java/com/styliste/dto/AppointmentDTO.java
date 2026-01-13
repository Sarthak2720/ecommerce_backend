package com.styliste.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentDTO {
    private String name;
    private Long id;
    private Long userId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String serviceType;
    private String notes;
    private String status;
    private LocalDateTime createdAt;
}
