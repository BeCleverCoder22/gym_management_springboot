package com.gym.management.gym_management.controller;



import com.gym.management.gym_management.entity.User;
import com.gym.management.gym_management.service.CustomUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final CustomUserDetailsService customUserDetailsService;

    public UserController(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(customUserDetailsService.getAllUsers()); // Récupérer tous les utilisateurs
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(customUserDetailsService.getUserById(id)); // Récupérer un utilisateur par son ID
    }


    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.ok(customUserDetailsService.createUser(user)); // Créer un nouvel utilisateur
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        return ResponseEntity.ok(customUserDetailsService.updateUser(id, user));  // Mettre à jour un utilisateur
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        customUserDetailsService.deleteUser(id);
        return ResponseEntity.noContent().build();  // Supprimer un utilisateur
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser() {
        User user = customUserDetailsService.getCurrentUser();
        user.setPassword(null); // Ne pas renvoyer le mot de passe
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me")
    public ResponseEntity<User> updateProfile(@RequestBody Map<String, String> payload) {
        User updatedUser = customUserDetailsService.updateProfile(payload.get("email"));
        updatedUser.setPassword(null);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> payload) {
        customUserDetailsService.changePassword(
                payload.get("oldPassword"),
                payload.get("newPassword")
        );
        return ResponseEntity.ok().build();
    }
}
