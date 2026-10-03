package com.gym.management.gym_management;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class GymManagementApplicationTests {
    private static final String TEST_JWT_SECRET = generateTestKey();
    private static final String TEST_PASSWORD = UUID.randomUUID() + "-A1";

    @Container
    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.secret-key", () -> TEST_JWT_SECRET);
    }

    @Test
    void contextLoads() {
    }

    @Test
    void publicRegistrationCannotAssignAdminAndResponsesDoNotExposePassword() throws Exception {
        String body = """
                {
                  "organizationName": "Security Test Gym",
                  "organizationSlug": "security-test-gym",
                  "username": "security-test-user",
                  "email": "security-test@example.com",
                  "password": "%s",
                  "role": "USER"
                }
                """.formatted(TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void userTokenCanAccessOwnProfileButNotAdminUserDirectory() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"organizationName":"Staff Test Gym",
                                 "organizationSlug":"staff-test-gym",
                                 "username":"gym-owner","email":"owner@example.com",
                                 "password":"%s"}
                                """.formatted(TEST_PASSWORD)))
                .andExpect(status().isCreated());

        String ownerLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"organizationSlug":"staff-test-gym","username":"gym-owner",
                                 "password":"%s"}
                                """.formatted(TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String ownerToken = objectMapper.readTree(ownerLogin).get("token").asText();
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"staff-test","email":"staff@example.com",
                                 "password":"%s","role":"USER"}
                                """.formatted(TEST_PASSWORD)))
                .andExpect(status().isCreated());

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"organizationSlug":"staff-test-gym",
                                 "username":"staff-test","password":"%s"}
                                """.formatted(TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode login = objectMapper.readTree(loginResponse);
        String token = login.get("token").asText();

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsReturnStructuredUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/customers"));
    }

    @Test
    void organizationTokensCannotReadOtherOrganizationsUsers() throws Exception {
        String alphaToken = registerAndLogin("Alpha Gym", "alpha-test-gym", "alpha-owner");
        String betaToken = registerAndLogin("Beta Gym", "beta-test-gym", "beta-owner");

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + alphaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("alpha-owner"));
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + betaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("beta-owner"));
    }

    @Test
    void customerSearchSupportsOmittedNullAndCombinedFilters() throws Exception {
        String token = registerAndLogin("Search Gym", "search-test-gym", "search-owner");

        createCustomer(token, "Alice", "Martin", "+33123456789", "alice@example.com");
        createCustomer(token, "Bob", "Durand", "+33987654321", "bob@example.com");
        createCustomer(token, "Carla", "Martin", "+33111111111", "carla@example.com");

        String auth = "Bearer " + token;
        mockMvc.perform(get("/api/customers").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].lastName").value("Martin"));

        mockMvc.perform(get("/api/customers").param("q", "ALICE").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("alice@example.com"));

        mockMvc.perform(get("/api/customers").param("q", "").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/customers").param("lastName", "Martin")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/customers").param("phone", "9876")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Bob"));

        mockMvc.perform(get("/api/customers").param("q", "martin")
                        .param("lastName", "martin").param("phone", "1234")
                        .param("sort", "registrationDate,desc").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Alice"));
    }

    private void createCustomer(
            String token, String firstName, String lastName, String phone, String email) throws Exception {
        mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"%s","lastName":"%s","phoneNumber":"%s","email":"%s"}
                                """.formatted(firstName, lastName, phone, email)))
                .andExpect(status().isCreated());
    }

    private String registerAndLogin(String organizationName, String slug, String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"organizationName":"%s","organizationSlug":"%s",
                                 "username":"%s","email":"%s@example.com","password":"%s"}
                                """.formatted(organizationName, slug, username, username, TEST_PASSWORD)))
                .andExpect(status().isCreated());
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"organizationSlug":"%s","username":"%s","password":"%s"}
                                """.formatted(slug, username, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private static String generateTestKey() {
        byte[] key = new byte[48];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
