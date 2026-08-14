package com.ecom.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService implements NotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;


    /*
     * send()
     * ────────
     * SimpleMailMessage is Spring's basic plain-text email builder —
     * sufficient for transactional emails like order confirmations and
     * receipts. A production system handling MARKETING emails or rich
     * HTML templates would use MimeMessageHelper instead, but plain
     * text is appropriate and simpler for the transactional notifications
     * this module sends.
     *
     * We catch and log failures rather than letting them propagate —
     * same philosophy as ProductCacheService's cache-write failures
     * (Part 18): a failed EMAIL send should never crash the Kafka
     * consumer or cause a message redelivery loop purely because an SMTP
     * server hiccupped. The consumer's acknowledgment (Part 19) still
     * happens even if the email itself silently fails here.
     */
    @Override
    public void send(String recipient, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            log.info("Email sent to {}: subject={}", recipient, subject);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", recipient, e.getMessage(), e);
        }
    }

}
