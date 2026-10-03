package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.Pack;
import com.gym.management.gym_management.exception.ResourceNotFoundException;
import com.gym.management.gym_management.repository.PackRepository;
import com.gym.management.gym_management.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PackService implements IPackService {
    private final PackRepository packRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;

    public PackService(PackRepository packRepository,
                       OrganizationRepository organizationRepository,
                       AuditService auditService) {
        this.packRepository = packRepository;
        this.organizationRepository = organizationRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Pack> getAllPacks(Pageable pageable) {
        return packRepository.findByOrganization_IdAndActiveTrue(
                TenantContext.requireOrganizationId(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Pack getPackById(Long id) {
        return packRepository.findByIdAndOrganization_Id(id, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Offre introuvable."));
    }


    @Override
    @Transactional
    public Pack addPack(String offerName, String description, int durationMonths,
                        java.math.BigDecimal monthlyPrice) {
        Pack pack = new Pack();
        pack.setOrganization(organizationRepository.getReferenceById(
                TenantContext.requireOrganizationId()));
        pack.setOfferName(offerName);
        pack.setDescription(description);
        pack.setDurationMonths(durationMonths);
        pack.setMonthlyPrice(monthlyPrice);
        Pack saved = packRepository.save(pack);
        auditService.record("PACK_CREATED", "PACK", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Pack updatePack(Long id, String offerName, String description, int durationMonths,
                           java.math.BigDecimal monthlyPrice) {
        return packRepository.findByIdAndOrganization_Id(id, TenantContext.requireOrganizationId()).map(pack -> {
            pack.setOfferName(offerName);
            pack.setDescription(description);
            pack.setDurationMonths(durationMonths);
            pack.setMonthlyPrice(monthlyPrice);
            Pack saved = packRepository.save(pack);
            auditService.record("PACK_UPDATED", "PACK", saved.getId());
            return saved;
        }).orElseThrow(() -> new ResourceNotFoundException("Offre introuvable."));
    }

    @Override
    @Transactional
    public void deletePack(Long id) {
        setActive(id, false);
    }

    @Override
    @Transactional
    public Pack setActive(Long id, boolean active) {
        Pack pack = getPackById(id);
        boolean changed = !Boolean.valueOf(active).equals(pack.getActive());
        pack.setActive(active);
        Pack saved = packRepository.save(pack);
        if (changed) {
            auditService.record(active ? "PACK_ACTIVATED" : "PACK_DEACTIVATED", "PACK", id);
        }
        return saved;
    }
}
