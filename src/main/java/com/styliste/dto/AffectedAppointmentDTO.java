package com.styliste.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class AffectedAppointmentDTO {
    private Long appointmentId;
    private String customerName;
    private String customerEmail;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String serviceType;
}
