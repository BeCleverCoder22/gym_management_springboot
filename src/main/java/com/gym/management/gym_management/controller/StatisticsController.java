package com.gym.management.gym_management.controller;

import com.gym.management.gym_management.dto.DashboardStatisticsResponse;
import com.gym.management.gym_management.dto.MonthlyRevenueResponse;
import com.gym.management.gym_management.exception.ResourceNotFoundException;
import com.gym.management.gym_management.service.IStatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/statistics")
@Tag(name = "Statistiques")
@SecurityRequirement(name = "bearerAuth")
public class StatisticsController {
    private final IStatisticsService statisticsService;

    public StatisticsController(IStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping
    public Map<String, Object> getStatistics() {
        DashboardStatisticsResponse dashboard = statisticsService.getDashboard();
        return Map.of(
                "totalActiveCustomers", dashboard.activeCustomers(),
                "monthlyRevenue", dashboard.estimatedMonthlyRevenue()
        );
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Récupérer les indicateurs principaux du tableau de bord")
    public DashboardStatisticsResponse getDashboard() {
        return statisticsService.getDashboard();
    }

    @GetMapping("/revenue")
    public Map<String, Object> getRevenueForPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        validatePeriod(startDate, endDate);
        return Map.of(
                "startDate", startDate,
                "endDate", endDate,
                "estimatedMonthlyValue", statisticsService.getRevenueForPeriod(startDate, endDate)
        );
    }

    @GetMapping("/revenue/monthly")
    public List<MonthlyRevenueResponse> getMonthlyRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        validatePeriod(startDate, endDate);
        return statisticsService.getRevenueByMonth(startDate, endDate);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportSubscriptions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate)
            throws IOException {
        validatePeriod(startDate, endDate);
        byte[] fileData = statisticsService.exportSubscriptions(startDate, endDate);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=subscriptions.csv")
                .body(fileData);
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La date de début doit être antérieure ou égale à la date de fin.");
        }
    }
}
