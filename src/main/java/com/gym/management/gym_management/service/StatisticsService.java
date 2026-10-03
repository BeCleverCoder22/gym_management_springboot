package com.gym.management.gym_management.service;

import com.gym.management.gym_management.dto.DashboardStatisticsResponse;
import com.gym.management.gym_management.dto.MonthlyRevenueResponse;
import com.gym.management.gym_management.dto.PackDistributionResponse;
import com.gym.management.gym_management.entity.Subscription;
import com.gym.management.gym_management.entity.SubscriptionArchive;
import com.gym.management.gym_management.entity.SubscriptionStatus;
import com.gym.management.gym_management.repository.CustomerRepository;
import com.gym.management.gym_management.repository.SubscriptionArchiveRepository;
import com.gym.management.gym_management.repository.SubscriptionRepository;
import com.opencsv.CSVWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.Clock;
import java.util.List;

@Service
public class StatisticsService implements IStatisticsService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final CustomerRepository customerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionArchiveRepository subscriptionArchiveRepository;
    private final Clock clock;

    public StatisticsService(
            CustomerRepository customerRepository,
            SubscriptionRepository subscriptionRepository,
            SubscriptionArchiveRepository subscriptionArchiveRepository,
            Clock clock) {
        this.customerRepository = customerRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionArchiveRepository = subscriptionArchiveRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatisticsResponse getDashboard() {
        LocalDate today = LocalDate.now(clock);
        LocalDate monthStart = YearMonth.from(today).atDay(1);
        Long organizationId = TenantContext.requireOrganizationId();
        List<PackDistributionResponse> distribution = subscriptionRepository
                .countSubscriptionsByPack(organizationId, SubscriptionStatus.CANCELLED).stream()
                .map(row -> new PackDistributionResponse(
                        (String) row[0], ((Number) row[1]).longValue()))
                .toList();

        return new DashboardStatisticsResponse(
                customerRepository.countByOrganization_Id(organizationId),
                subscriptionRepository.countActiveCustomersAsOf(organizationId, SubscriptionStatus.ACTIVE, today),
                customerRepository.countByOrganization_IdAndRegistrationDateBetween(organizationId, monthStart, today),
                subscriptionRepository.countActiveAsOf(organizationId, SubscriptionStatus.ACTIVE, today),
                subscriptionRepository.countExpiredAsOf(organizationId, SubscriptionStatus.CANCELLED, today),
                subscriptionRepository.countExpiringBetween(
                        organizationId, SubscriptionStatus.ACTIVE, today, today.plusDays(30)),
                subscriptionRepository.countSoldBetween(
                        organizationId, monthStart, today, SubscriptionStatus.CANCELLED),
                getMonthlyRevenue(),
                distribution
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getMonthlyRevenue() {
        BigDecimal revenue = subscriptionRepository.calculateMonthlyRevenue(
                TenantContext.requireOrganizationId(), LocalDate.now(clock), SubscriptionStatus.ACTIVE);
        return revenue == null ? ZERO : revenue;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getRevenueForPeriod(LocalDate startDate, LocalDate endDate) {
        BigDecimal revenue = subscriptionRepository.sumMonthlyValueStartedBetween(
                TenantContext.requireOrganizationId(), startDate, endDate, SubscriptionStatus.CANCELLED);
        return revenue == null ? ZERO : revenue;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyRevenueResponse> getRevenueByMonth(LocalDate startDate, LocalDate endDate) {
        return subscriptionRepository.sumMonthlyValueByMonth(
                        TenantContext.requireOrganizationId(), startDate, endDate).stream()
                .map(row -> new MonthlyRevenueResponse(
                        toLocalDate(row[0]), (BigDecimal) row[1]))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportSubscriptions(LocalDate startDate, LocalDate endDate) throws IOException {
        List<Subscription> subscriptions =
                subscriptionRepository.findSubscriptionsForPeriod(
                        TenantContext.requireOrganizationId(), startDate, endDate);
        List<SubscriptionArchive> archivedSubscriptions =
                subscriptionArchiveRepository.findSubscriptionsArchiveForPeriod(
                        TenantContext.requireOrganizationId(), startDate, endDate);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(
                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            writer.writeNext(new String[]{
                    "ID Abonnement", "Client", "Pack", "Date de début", "Date de fin",
                    "Prix mensuel", "Statut"
            });

            for (Subscription subscription : subscriptions) {
                writer.writeNext(new String[]{
                        String.valueOf(subscription.getId()),
                        safeCsv(subscription.getCustomer().getLastName() + " "
                                + subscription.getCustomer().getFirstName()),
                        safeCsv(subscription.getOfferNameAtPurchase()),
                        String.valueOf(subscription.getStartDate()),
                        String.valueOf(subscription.getEndDate()),
                        String.valueOf(subscription.getMonthlyPriceAtPurchase()),
                        subscription.getStatus().name()
                });
            }

            for (SubscriptionArchive subscription : archivedSubscriptions) {
                writer.writeNext(new String[]{
                        String.valueOf(subscription.getId()),
                        safeCsv(subscription.getCustomer().getLastName() + " "
                                + subscription.getCustomer().getFirstName()),
                        safeCsv(subscription.getOfferNameAtPurchase()),
                        String.valueOf(subscription.getStartDate()),
                        String.valueOf(subscription.getEndDate()),
                        String.valueOf(subscription.getMonthlyPriceAtPurchase()),
                        SubscriptionStatus.CANCELLED.name()
                });
            }
        }
        return outputStream.toByteArray();
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        throw new IllegalStateException("Unexpected date type in monthly revenue query.");
    }

    private String safeCsv(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        char first = value.charAt(0);
        return first == '=' || first == '+' || first == '-' || first == '@'
                ? "'" + value
                : value;
    }
}
