package com.gym.management.gym_management.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Identifiants invalides.");
    }
}
