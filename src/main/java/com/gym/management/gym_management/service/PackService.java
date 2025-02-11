package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.Pack;
import com.gym.management.gym_management.repository.PackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PackService implements IPackService{
    @Autowired
    private PackRepository packRepository;

    @Override
    public List<Pack> getAllPacks() {
        return packRepository.findAll(); // Récupérer toutes les offres
    }

    @Override
    public Pack getPackById(Long id) {
        return packRepository.findById(id) // Récupérer une offre
                .orElseThrow(() -> new RuntimeException("Pack non trouvé avec l'ID : " + id));
    }


    @Override
    public Pack addPack(Pack pack) {
        return packRepository.save(pack); // Ajouter une nouvelle offre
    }

    @Override
    public Pack updatePack(Long id, Pack updatedPack) {
        return packRepository.findById(id).map(pack -> {
            pack.setOfferName(updatedPack.getOfferName());
            pack.setDurationMonths(updatedPack.getDurationMonths());
            pack.setMonthlyPrice(updatedPack.getMonthlyPrice());
            return packRepository.save(pack);
        }).orElseThrow(() -> new RuntimeException("Pack non trouvé avec l'ID : " + id)); // Modifier une offre
    }

    @Override
    public void deletePack(Long id) {
        packRepository.deleteById(id); // Supprimer une offre
    }
}
