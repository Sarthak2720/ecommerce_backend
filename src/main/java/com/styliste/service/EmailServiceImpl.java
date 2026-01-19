package com.styliste.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService{

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ================= APPROVED =================

    @Async
    @Override
    public void sendAppointmentApprovedEmail(
            String email,
            String name,
            String appointmentDate,
            String appointmentTime,
            String serviceName
    ) {
        String subject = "✅ Appointment Confirmed";
        String body = buildApprovalEmail(
                name,
                appointmentDate,
                appointmentTime,
                serviceName
        );
        sendEmail(email, subject, body);
    }

    // ================= REJECTED =================

    @Async
    @Override
    public void sendAppointmentRejectedEmail(
            String email,
            String name
    ) {
        String subject = "❌ Appointment Request Rejected";
        String body = buildRejectionEmail(name);
        sendEmail(email, subject, body);
    }

    // ================= CORE MAIL =================

    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Email sent successfully to {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to, e);
        }
    }

    // ================= TEMPLATES =================

    private String buildApprovalEmail(
            String name,
            String date,
            String time,
            String service
    ) {
        return """
        <html>
            <body style="font-family:Arial,sans-serif;">
                <h2 style="color:#2ecc71;">Appointment Confirmed</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>Your appointment has been <strong>confirmed</strong>.</p>
                <p>
                    <b>Date:</b> %s<br/>
                    <b>Time:</b> %s<br/>
                    <b>Service:</b> %s
                </p>
                <p>We look forward to serving you.</p>
                <br/>
                <p>Regards,<br/><b>Styliste Team</b></p>
            </body>
        </html>
        """.formatted(name, date, time, service);
    }

    private String buildRejectionEmail(String name) {
        return """
        <html>
            <body style="font-family:Arial,sans-serif;">
                <h2 style="color:#e74c3c;">Appointment Not Available</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>We’re sorry to inform you that your appointment request could not be accepted.</p>
                <p>The selected slot is currently <strong>fully booked</strong>.</p>
                <p>Please try booking another available slot.</p>
                <br/>
                <p>Regards,<br/><b>Styliste Team</b></p>
            </body>
        </html>
        """.formatted(name);
    }
}
