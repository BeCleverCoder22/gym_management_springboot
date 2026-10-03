package com.gym.management.gym_management.service;

public interface NotificationDelivery {
    void send(String recipient, String subject, String body);
}
