package com.gym.management.gym_management.controller;

import com.gym.management.gym_management.entity.Subscription;
import com.gym.management.gym_management.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    // Endpoint pour récupérer le nombre total de clients actifs  et le Chiffre d'affaires mensuel estimé
    @GetMapping
    public Map<String, Object> getStatistics() {
        return Map.of(
                "totalActiveCustomers", statisticsService.getActiveCustomersCount(),
                "monthlyRevenue", statisticsService.getMonthlyRevenue()
        );
    }

    // Endpoint pour exporter les abonnements sur une période donnée
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportSubscriptions(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        try {
            byte[] fileData = statisticsService.exportSubscriptions(startDate, endDate);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=subscriptions.csv");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }
}
