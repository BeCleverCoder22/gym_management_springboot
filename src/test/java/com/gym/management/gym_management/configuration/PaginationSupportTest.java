package com.gym.management.gym_management.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PaginationSupportTest {
    @Test
    void buildsBoundedPageWithWhitelistedStableSort() {
        Pageable pageable = PaginationSupport.create(2, 25, "lastName,desc", Set.of("id", "lastName"));

        assertEquals(2, pageable.getPageNumber());
        assertEquals(25, pageable.getPageSize());
        assertEquals("lastName", pageable.getSort().getOrderFor("lastName").getProperty());
        assertEquals("id", pageable.getSort().getOrderFor("id").getProperty());
    }

    @Test
    void rejectsInvalidPageSizeAndUnapprovedSortField() {
        assertThrows(IllegalArgumentException.class,
                () -> PaginationSupport.create(0, 101, "id,asc", Set.of("id")));
        assertThrows(IllegalArgumentException.class,
                () -> PaginationSupport.create(0, 20, "password,asc", Set.of("id")));
    }
}
