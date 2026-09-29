package com.gym.management.gym_management.security;

import com.gym.management.gym_management.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.time.Clock;
import java.util.Collection;
import java.util.List;

public class GymUserDetails implements UserDetails {
    private final String username;
    private final String password;
    private final String role;
    private final boolean enabled;
    private final LocalDateTime lockedUntil;
    private final int tokenVersion;
    private final Clock clock;

    public GymUserDetails(User user, Clock clock) {
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.role = user.getRole().name();
        this.enabled = !Boolean.FALSE.equals(user.getEnabled());
        this.lockedUntil = user.getLockedUntil();
        this.tokenVersion = user.getTokenVersion();
        this.clock = clock;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || !lockedUntil.isAfter(LocalDateTime.now(clock));
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }
}
