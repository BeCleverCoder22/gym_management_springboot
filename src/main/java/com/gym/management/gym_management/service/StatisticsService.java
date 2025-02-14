package com.gym.management.gym_management.service;


import com.gym.management.gym_management.entity.Subscription;
import com.gym.management.gym_management.entity.SubscriptionArchive;
import com.gym.management.gym_management.repository.CustomerRepository;
import com.gym.management.gym_management.repository.SubscriptionArchiveRepository;
import com.gym.management.gym_management.repository.SubscriptionRepository;
import com.opencsv.CSVWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
public class StatisticsService implements IStatisticsService{

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionArchiveRepository subscriptionArchiveRepository;

    @Override
    public long getActiveCustomersCount() {
        return customerRepository.countByActiveSubscription(true); // Nombre total de clients actifs
    }

    @Override
    public double getMonthlyRevenue() {
        Double revenue = subscriptionRepository.calculateMonthlyRevenue(); // Chiffre d'affaires mensuel estimé
        return (revenue != null) ? revenue : 0.0;
    }

    @Override
    public byte[] exportSubscriptions(LocalDate startDate, LocalDate endDate) throws IOException {
        List<Subscription> activeSubscriptions = subscriptionRepository.findSubscriptionsForPeriod(startDate, endDate);
        List<SubscriptionArchive> archivedSubscriptions = subscriptionArchiveRepository.findSubscriptionsArchiveForPeriod(startDate, endDate);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(outputStream))) {
            // En-tête CSV
            writer.writeNext(new String[]{
                    "ID Abonnement",
                    "Client",
                    "Pack",
                    "Date de début",
                    "Date de fin",
                    "Prix mensuel",
                    "Statut"
            });

            // Données des abonnements actifs
            for (Subscription sub : activeSubscriptions) {
                writer.writeNext(new String[]{
                        sub.getId().toString(),
                        sub.getCustomer().getLastName() + " " + sub.getCustomer().getFirstName(),
                        sub.getPack().getOfferName(),
                        sub.getStartDate().toString(),
                        sub.getEndDate() != null ? sub.getEndDate().toString() : "En cours",
                        String.valueOf(sub.getPack().getMonthlyPrice()),
                        "Actif"
                });
            }

            // Données des abonnements archivés
            for (SubscriptionArchive sub : archivedSubscriptions) {
                writer.writeNext(new String[]{
                        sub.getId().toString(),
                        sub.getCustomer().getLastName() + " " + sub.getCustomer().getFirstName(),
                        sub.getPack().getOfferName(),
                        sub.getStartDate().toString(),
                        sub.getEndDate() != null ? sub.getEndDate().toString() : "En cours",
                        String.valueOf(sub.getPack().getMonthlyPrice()),
                        "Archivé"
                });
            }
        }

        return outputStream.toByteArray();
    }
}
