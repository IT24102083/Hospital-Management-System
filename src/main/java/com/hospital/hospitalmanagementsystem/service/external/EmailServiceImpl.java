package com.hospital.hospitalmanagementsystem.service.external;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private final String FROM_EMAIL = "noreply@hospital.com";

    @Override
    public void sendEmail(String to, String subject, String message) {
        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(FROM_EMAIL);
            email.setTo(to);
            email.setSubject(subject);
            email.setText(message);
            mailSender.send(email);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(FROM_EMAIL);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send HTML email: " + e.getMessage());
        }
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String message, String attachmentPath) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(FROM_EMAIL);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(message);

            // Add attachment
            File file = new File(attachmentPath);
            if (file.exists()) {
                helper.addAttachment(file.getName(), file);
            }

            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.err.println("Failed to send email with attachment: " + e.getMessage());
        }
    }

    @Override
    public boolean isEmailServiceAvailable() {
        return mailSender != null;
    }
}