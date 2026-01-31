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

import java.util.List;

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

    // Add to EmailServiceImpl class

    @Async
    @Override
    public void sendUnavailabilityApologyEmail(
            String email,
            String name,
            String blockedDate,
            String blockedTime,
            List<String> alternativeSlots
    ) {
        String subject = "🔔 Important: Appointment Schedule Change";
        String body = buildUnavailabilityApologyEmail(
                name,
                blockedDate,
                blockedTime,
                alternativeSlots
        );
        sendEmail(email, subject, body);
    }

    private String buildUnavailabilityApologyEmail(
            String name,
            String date,
            String time,
            List<String> alternatives
    ) {
        StringBuilder alternativesHtml = new StringBuilder();

        if (alternatives != null && !alternatives.isEmpty()) {
            alternativesHtml.append("<ul style='margin-top:10px;'>");
            for (String slot : alternatives) {
                alternativesHtml.append("<li>").append(slot).append("</li>");
            }
            alternativesHtml.append("</ul>");
        } else {
            alternativesHtml.append("<p>Please contact us to reschedule.</p>");
        }

        return """
    <html>
        <body style="font-family:Arial,sans-serif; padding:20px;">
            <h2 style="color:#f39c12;">Appointment Rescheduling Notice</h2>
            <p>Dear <strong>%s</strong>,</p>
            
            <p>We sincerely apologize for the inconvenience, but we need to inform you 
            that your scheduled appointment is no longer available due to unforeseen circumstances.</p>
            
            <div style="background:#f8f9fa; padding:15px; border-left:4px solid #f39c12; margin:20px 0;">
                <p style="margin:0;"><b>Your Original Appointment:</b></p>
                <p style="margin:5px 0;">📅 Date: <strong>%s</strong></p>
                <p style="margin:5px 0;">⏰ Time: <strong>%s</strong></p>
            </div>
            
            <p><strong>Alternative Available Slots:</strong></p>
            %s
            
            <p>If any of these slots work for you, please reply to this email or contact us, 
            and we'll be happy to reschedule your appointment immediately.</p>
            
            <p>We deeply regret any inconvenience this may cause and appreciate your understanding.</p>
            
            <br/>
            <p>Warm regards,<br/><b>Styliste Team</b></p>
        </body>
    </html>
    """.formatted(name, date, time, alternativesHtml.toString());
    }

    @Async
    @Override
    public void sendAvailabilityRestoredEmail(
            String email,
            String name,
            String date,
            String time,
            String serviceName
    ) {
        String subject = "✅ Your Appointment Is Confirmed";
        String body = """
        <html>
          <body style="font-family:Arial; padding:20px;">
            <h2 style="color:#27ae60;">Good News!</h2>

            <p>Dear <b>%s</b>,</p>

            <p>We’re happy to inform you that we are now available again.</p>

            <div style="background:#f8f9fa;padding:15px;border-left:4px solid #27ae60;">
              <p><b>Your appointment remains unchanged:</b></p>
              <p>📅 Date: <b>%s</b></p>
              <p>⏰ Time: <b>%s</b></p>
              <p>💇 Service: <b>%s</b></p>
            </div>

            <p>No action is required from your side.</p>

            <p>We look forward to serving you!</p>

            <br/>
            <p>Warm regards,<br/><b>Styliste Team</b></p>
          </body>
        </html>
        """.formatted(name, date, time, serviceName);

        sendEmail(email, subject, body);
    }

    @Async
    @Override
    public void sendPasswordResetOtpEmail(String email, String otp) {
        String subject = "🔐 Password Reset OTP - Styliste";
        String body = """
    <html>
        <body style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
            <div style="max-width: 600px; margin: auto; border: 1px solid #eee; padding: 20px; border-radius: 10px;">
                <h2 style="color: #2c3e50; text-align: center;">Password Reset Request</h2>
                <p>Hello,</p>
                <p>We received a request to reset your password for your <strong>Styliste</strong> account.</p>
                <p>Please use the following One-Time Password (OTP) to proceed:</p>
                
                <div style="background: #f4f4f4; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; letter-spacing: 5px; color: #2980b9; margin: 20px 0;">
                    %s
                </div>
                
                <p style="color: #e74c3c;"><strong>Note:</strong> This OTP is valid for 10 minutes. If you did not request this, please ignore this email.</p>
                
                <br/>
                <p>Regards,<br/><b>Styliste Team</b></p>
            </div>
        </body>
    </html>
    """.formatted(otp);

        sendEmail(email, subject, body);
    }

}
