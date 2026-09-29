package com.gym.management.gym_management.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlyRevenueResponse(LocalDate month, BigDecimal monthlyValue) {
}
