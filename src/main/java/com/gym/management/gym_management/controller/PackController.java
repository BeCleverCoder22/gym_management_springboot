package com.gym.management.gym_management.controller;

import com.gym.management.gym_management.dto.PackRequest;
import com.gym.management.gym_management.dto.PackResponse;
import com.gym.management.gym_management.dto.PackStatusRequest;
import com.gym.management.gym_management.dto.PageResponse;
import com.gym.management.gym_management.configuration.PaginationSupport;
import com.gym.management.gym_management.service.PackService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Set;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/packs")
@Tag(name = "Offres")
@SecurityRequirement(name = "bearerAuth")
public class PackController {
    private final PackService packService;

    public PackController(PackService packService) {
        this.packService = packService;
    }

    @GetMapping
    @Operation(summary = "Lister les offres actives avec pagination")
    public PageResponse<PackResponse> getAllPacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Pageable pageable = PaginationSupport.create(
                page, size, sort, Set.of("id", "offerName", "durationMonths", "monthlyPrice", "createdAt"));
        return PageResponse.from(packService.getAllPacks(pageable), PackResponse::from);
    }

    @GetMapping("/{id}")
    public PackResponse getPackById(@PathVariable Long id) {
        return PackResponse.from(packService.getPackById(id));
    }

    @PostMapping
    public ResponseEntity<PackResponse> addPack(@Valid @RequestBody PackRequest request) {
        PackResponse pack = PackResponse.from(packService.addPack(
                request.offerName(), request.description(), request.durationMonths(), request.monthlyPrice()));
        return ResponseEntity.created(URI.create("/api/packs/" + pack.id())).body(pack);
    }

    @PutMapping("/{id}")
    public PackResponse updatePack(
            @PathVariable Long id, @Valid @RequestBody PackRequest request) {
        return PackResponse.from(packService.updatePack(
                id, request.offerName(), request.description(),
                request.durationMonths(), request.monthlyPrice()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Désactiver une offre en préservant les références historiques")
    public ResponseEntity<Void> deactivatePack(@PathVariable Long id) {
        packService.deletePack(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public PackResponse setPackStatus(
            @PathVariable Long id, @Valid @RequestBody PackStatusRequest request) {
        return PackResponse.from(packService.setActive(id, request.active()));
    }
}
