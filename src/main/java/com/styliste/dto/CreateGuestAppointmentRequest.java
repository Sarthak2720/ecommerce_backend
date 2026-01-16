package com.styliste.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreateGuestAppointmentRequest {

    @NotBlank
    private String guestName;

    @Email
    @NotBlank
    private String guestEmail;

    @NotBlank
    private String guestPhone;

    @NotNull
    private LocalDate appointmentDate;

    @NotNull
    private LocalTime appointmentTime;

    @NotBlank
    private String serviceType;

    private String notes;
}
