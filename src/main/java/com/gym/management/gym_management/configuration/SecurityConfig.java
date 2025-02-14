package com.gym.management.gym_management.configuration;

import com.gym.management.gym_management.filter.JwtFilter;
import com.gym.management.gym_management.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }


    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtFilter jwtFilter(CustomUserDetailsService userDetailsService, JwtUtils jwtUtils) {
        return new JwtFilter(userDetailsService, jwtUtils);
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, PasswordEncoder passwordEncoder) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(customUserDetailsService).passwordEncoder(passwordEncoder);
        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter, LoginSuccessHandler loginSuccessHandler) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // ✅ Activation du CORS
                .csrf(AbstractHttpConfigurer::disable) // ✅ Désactivation de CSRF pour l'API
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // ✅ Accès libre aux routes d'authentification
                        .requestMatchers(HttpMethod.GET, "/api/users/**").authenticated() // ✅ Tous les utilisateurs connectés peuvent voir la liste
                        .requestMatchers(HttpMethod.POST, "/api/users").hasAuthority("ADMIN") // ✅ Seuls les admins peuvent créer un utilisateur
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasAuthority("ADMIN") // ✅ Seuls les admins peuvent modifier un utilisateur
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAuthority("ADMIN") // ✅ Seuls les admins peuvent supprimer un utilisateur
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // ✅ Pas de session, API REST sécurisée avec JWT
                .formLogin(login -> login.successHandler(loginSuccessHandler)) // ✅ Gestionnaire de succès après connexion
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class) // ✅ Ajout du filtre JWT avant UsernamePasswordAuthenticationFilter
                .build();
    }



    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("http://localhost:4200"); // Autorise Angular
        configuration.addAllowedMethod("*"); // Autorise toutes les méthodes (GET, POST, etc.)
        configuration.addAllowedHeader("*"); // Autorise tous les headers
        configuration.setAllowCredentials(true); // Autorise les cookies et authentification

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
