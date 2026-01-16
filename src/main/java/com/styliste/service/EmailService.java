package com.styliste.service;

import com.styliste.entity.Appointment;

public interface EmailService {
    void sendAppointmentApprovedEmail(Appointment appointment);
    void sendAppointmentRejectedEmail(Appointment appointment);
}
