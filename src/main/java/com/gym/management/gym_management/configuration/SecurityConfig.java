package com.gym.management.gym_management.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.management.gym_management.exception.ApiError;
import com.gym.management.gym_management.filter.JwtFilter;
import com.gym.management.gym_management.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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

import java.time.Instant;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final String allowedOrigin;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          @Value("${app.cors.allowed-origin:http://localhost:4200}") String allowedOrigin) {
        this.customUserDetailsService = customUserDetailsService;
        this.allowedOrigin = allowedOrigin;
    }


    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder(12);
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
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtFilter jwtFilter, ObjectMapper objectMapper) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**",
                                "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/users/me", "/api/users/change-password").authenticated()
                        .requestMatchers("/api/users/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/audit/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/packs/**").authenticated()
                        .requestMatchers("/api/packs/**").hasAuthority("ADMIN")
                        .requestMatchers("/actuator/**").hasAuthority("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeSecurityError(objectMapper, response, request,
                                        HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED",
                                        "Authentification requise."))
                        .accessDeniedHandler((request, response, exception) ->
                                writeSecurityError(objectMapper, response, request,
                                        HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN",
                                        "Accès interdit.")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private void writeSecurityError(
            ObjectMapper objectMapper,
            HttpServletResponse response,
            jakarta.servlet.http.HttpServletRequest request,
            int status,
            String error,
            String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), new ApiError(
                Instant.now(), status, error, message, request.getRequestURI(), Map.of()));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin(allowedOrigin);
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
