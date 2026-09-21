package com.crm.matrix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendCredentialsEmail(String toEmail, String employeeCode, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Welcome to CRM Matrix - Your Account Details");

        String emailBody = "Welcome to CRM Matrix!\n\n" +
                "Your account has been successfully created. Here are your login details:\n\n" +
                "Username (Employee Code): " + employeeCode + "\n" +
                "Temporary Password: " + temporaryPassword + "\n\n" +
                "For security reasons, please log in and change your password immediately.\n\n" +
                "Best Regards,\n" +
                "CRM Matrix Admin Team";

        message.setText(emailBody);

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
            // You can choose to throw a custom exception here or just log it so the
            // user creation doesn't fail entirely if the email server is temporarily down.
        }

    }
    public void sendPasswordResetEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("CRM Matrix - Password Reset OTP");

        String emailBody = "Hello,\n\n" +
                "We received a request to reset your password. Here is your 6-digit OTP:\n\n" +
                "OTP: " + otp + "\n\n" +
                "This OTP is valid for 10 minutes. If you did not request this, please ignore this email.\n\n" +
                "Best Regards,\n" +
                "CRM Matrix Security Team";

        message.setText(emailBody);

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send OTP email to " + toEmail + ": " + e.getMessage());
        }
    }
}