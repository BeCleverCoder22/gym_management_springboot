package com.gym.management.gym_management.controller;

import com.gym.management.gym_management.entity.Pack;
import com.gym.management.gym_management.service.PackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/packs")
public class PackController {
    @Autowired
    private PackService packService;

    @GetMapping
    public List<Pack> getAllPacks() {
        return packService.getAllPacks();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pack> getPackById(@PathVariable Long id) {
        Pack pack = packService.getPackById(id);
        return ResponseEntity.ok(pack);
    }

    @PostMapping
    public Pack addPack(@RequestBody Pack pack) {
        return packService.addPack(pack);
    }

    @PutMapping("/{id}")
    public Pack updatePack(@PathVariable Long id, @RequestBody Pack updatedPack) {
        return packService.updatePack(id, updatedPack);
    }

    @DeleteMapping("/{id}")
    public void deletePack(@PathVariable Long id) {
        packService.deletePack(id);
    }
}
