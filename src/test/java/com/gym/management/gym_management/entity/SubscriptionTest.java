package com.gym.management.gym_management.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubscriptionTest {
    @Test
    void snapshotsPackTermsAndCalculatesEndDateUsingCalendarMonths() {
        Pack pack = new Pack();
        pack.setOfferName("Monthly");
        pack.setDurationMonths(1);
        pack.setMonthlyPrice(new BigDecimal("29.90"));

        Subscription subscription = new Subscription();
        subscription.setPack(pack);
        subscription.setStartDate(LocalDate.of(2026, 1, 31));

        pack.setOfferName("Changed name");
        pack.setMonthlyPrice(new BigDecimal("39.90"));

        assertEquals(LocalDate.of(2026, 2, 28), subscription.getEndDate());
        assertEquals("Monthly", subscription.getOfferNameAtPurchase());
        assertEquals(new BigDecimal("29.90"), subscription.getMonthlyPriceAtPurchase());
    }
}
