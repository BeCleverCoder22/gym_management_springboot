package com.gym.management.gym_management.dto;

import com.gym.management.gym_management.entity.Pack;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PackResponse(
        Long id,
        String offerName,
        String description,
        int durationMonths,
        BigDecimal monthlyPrice,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PackResponse from(Pack pack) {
        return new PackResponse(
                pack.getId(),
                pack.getOfferName(),
                pack.getDescription(),
                pack.getDurationMonths(),
                pack.getMonthlyPrice(),
                !Boolean.FALSE.equals(pack.getActive()),
                pack.getCreatedAt(),
                pack.getUpdatedAt()
        );
    }
}
