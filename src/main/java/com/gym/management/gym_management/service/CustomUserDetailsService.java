package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.User;
import com.gym.management.gym_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomUserDetailsService(UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole())));
    }


    public List<User> getAllUsers() {
        return userRepository.findAll(); // récupérer tous les utilisateurs
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    @Transactional
    public User createUser(User user) {
        // Vérifier si l'email est déjà utilisé
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("L'email est déjà utilisé !");
        }

        // Vérifier si le nom d'utilisateur est déjà pris
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Le nom d'utilisateur est déjà pris !");
        }

        // Hasher le mot de passe avant de sauvegarder
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, User updatedUser) {
        User existingUser = getUserById(id);

        // 🔹 Vérifier et mettre à jour les champs seulement s'ils ne sont pas `null`
        if (updatedUser.getUsername() != null && !updatedUser.getUsername().isEmpty()) {
            existingUser.setUsername(updatedUser.getUsername());
        }

        if (updatedUser.getEmail() != null && !updatedUser.getEmail().isEmpty()) {
            existingUser.setEmail(updatedUser.getEmail());
        }

        if (updatedUser.getRole() != null && !updatedUser.getRole().isEmpty()) {
            existingUser.setRole(updatedUser.getRole());
        }

        // 🔹 Sauvegarder les modifications uniquement si un champ a été modifié
        return userRepository.save(existingUser);
    }



    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username); // Récupère l'utilisateur actuellement connecté.
    }


    @Transactional
    public User updateProfile(String email) {
        User user = getCurrentUser();
        if (user == null) {
            throw new UsernameNotFoundException("Utilisateur non trouvé !");
        }
        user.setEmail(email);
        return userRepository.save(user);  // Met à jour l'email de l'utilisateur actuellement connecté.
    }

    @Transactional
    public void changePassword(String oldPassword, String newPassword) { // Change le mot de passe de l'utilisateur après vérification.
        User user = getCurrentUser();
        if (user == null) {
            throw new UsernameNotFoundException("Utilisateur non trouvé !");
        }

        // Vérifier si l'ancien mot de passe est correct
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Ancien mot de passe incorrect !");
        }

        // Mettre à jour le mot de passe
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);  // Supprimer un utilisateur
    }
}
