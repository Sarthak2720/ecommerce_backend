package com.styliste.service;

import com.styliste.entity.Appointment;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;


@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    @Async
    public void sendAppointmentApprovedEmail(Appointment appointment) {
        String subject = "✅ Appointment Confirmed";
        String body = buildApprovalEmail(appointment);

        sendEmail(
                appointment.getUser().getEmail(),
                subject,
                body
        );
    }

    @Override
    @Async
    public void sendAppointmentRejectedEmail(Appointment appointment) {
        String subject = "❌ Appointment Request Rejected";
        String body = buildRejectionEmail(appointment);

        sendEmail(
                appointment.getUser().getEmail(),
                subject,
                body
        );
    }

    // ------------------ CORE MAIL SENDER ------------------

    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML

            mailSender.send(message);
            log.info("Email sent successfully to {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to, e);
            // DO NOT throw exception → appointment flow should not break
        }
    }

    // ------------------ EMAIL TEMPLATES ------------------

    private String buildApprovalEmail(Appointment appointment) {
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
        """.formatted(
                appointment.getUser().getName(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime(),
                appointment.getServiceType().name()
        );
    }

    private String buildRejectionEmail(Appointment appointment) {
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
        """.formatted(
                appointment.getUser().getName()
        );
    }
}
