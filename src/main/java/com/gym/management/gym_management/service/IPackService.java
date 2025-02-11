package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.Pack;

import java.util.List;

public interface IPackService {
    // Récupérer toutes les offres
    public List<Pack> getAllPacks();
    // Récupérer une offre
    public Pack getPackById(Long id);
    // Ajouter une nouvelle offre
    public Pack addPack(Pack pack);
    // Modifier une offre
    public Pack updatePack(Long id, Pack updatedPack);
    // Supprimer une offre
    public void deletePack(Long id);
}
