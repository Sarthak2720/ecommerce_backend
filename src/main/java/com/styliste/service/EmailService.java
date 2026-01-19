package com.styliste.service;

import com.styliste.entity.Appointment;
import org.springframework.scheduling.annotation.Async;

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
}
