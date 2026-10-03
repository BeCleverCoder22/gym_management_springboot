package com.gym.management.gym_management.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.notifications.email.enabled", havingValue = "true")
public class SmtpNotificationDelivery implements NotificationDelivery {
    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpNotificationDelivery(
            JavaMailSender mailSender,
            @org.springframework.beans.factory.annotation.Value("${app.notifications.email.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(String recipient, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
