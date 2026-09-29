package com.gym.management.gym_management.configuration;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PaginationSupport {
    public static final int MAX_PAGE_SIZE = 100;

    private PaginationSupport() {
    }

    public static Pageable create(int page, int size, String sort, Set<String> allowedFields) {
        if (page < 0) {
            throw new IllegalArgumentException("Le numéro de page doit être supérieur ou égal à zéro.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("La taille de page doit être comprise entre 1 et 100.");
        }

        String[] parts = sort.split(",", -1);
        if (parts.length != 2 || !allowedFields.contains(parts[0])) {
            throw new IllegalArgumentException("Le tri demandé n'est pas autorisé.");
        }

        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("La direction de tri doit être asc ou desc.");
        }
        Sort requested = Sort.by(direction, parts[0]);
        if (!parts[0].equals("id")) {
            requested = requested.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(page, size, requested);
    }
}
