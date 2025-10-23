package com.hospital.hospitalmanagementsystem.service.external;

public interface EmailService {

    void sendEmail(String to, String subject, String message);

    void sendHtmlEmail(String to, String subject, String htmlContent);

    void sendEmailWithAttachment(String to, String subject, String message, String attachmentPath);

    boolean isEmailServiceAvailable();
}