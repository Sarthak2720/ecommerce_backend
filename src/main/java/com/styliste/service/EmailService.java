package com.styliste.service;

import com.styliste.entity.Appointment;
import org.springframework.scheduling.annotation.Async;

import java.util.List;

public interface EmailService {

    @Async
    void sendAppointmentApprovedEmail(
            String email,
            String name,
            String appointmentDate,
            String appointmentTime,
            String serviceName
    );

    @Async
    void sendAppointmentRejectedEmail(
            String email,
            String name
    );
    @Async
    void sendUnavailabilityApologyEmail(String email, String name, String blockedDate, String blockedTime, List<String> alternativeSlots);

    @Async
    void sendAvailabilityRestoredEmail(
            String email,
            String name,
            String appointmentDate,
            String appointmentTime,
            String serviceName
    );
    @Async
    void sendPasswordResetOtpEmail(String email, String otp);

}
