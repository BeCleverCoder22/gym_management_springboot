package com.gym.management.gym_management.service;

import java.util.List;
import com.gym.management.gym_management.entity.Pack;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IPackService {
    // Récupérer toutes les offres
    public Page<Pack> getAllPacks(Pageable pageable);
    // Récupérer une offre
    public Pack getPackById(Long id);
    // Ajouter une nouvelle offre
    public Pack addPack(String offerName, String description, int durationMonths, java.math.BigDecimal monthlyPrice);
    // Modifier une offre
    public Pack updatePack(Long id, String offerName, String description, int durationMonths, java.math.BigDecimal monthlyPrice);
    public Pack setActive(Long id, boolean active);
    // Supprimer une offre
    public void deletePack(Long id);
}
