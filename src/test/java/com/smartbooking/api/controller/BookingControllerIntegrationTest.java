package com.smartbooking.api.controller;

import com.smartbooking.config.security.UserPrincipal;
import com.smartbooking.domain.model.Resource;
import com.smartbooking.domain.model.User;
import com.smartbooking.domain.repository.ResourceRepository;
import com.smartbooking.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Test
    void shouldCreateBookingThroughApi() throws Exception {

        // Utilisateur de test existant
        User user = userRepository.findByEmail("user@smartbooking.com")
                .orElseThrow();

        // Nouvelle ressource créée uniquement pour ce test
        Resource resource = Resource.builder()
                .name("Salle test API " + System.nanoTime())
                .type("ROOM")
                .description("Ressource utilisée pour le test API")
                .capacity(1)
                .isActive(true)
                .build();

        Resource savedResource = resourceRepository.save(resource);

        // Authentification du véritable UserPrincipal
        UserPrincipal userPrincipal = new UserPrincipal(user);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userPrincipal,
                        null,
                        userPrincipal.getAuthorities()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        try {
            mockMvc.perform(
                    post("/api/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(
                                    "Idempotency-Key",
                                    "api-test-" + System.nanoTime()
                            )
                            .content("""
                                    {
                                        "resourceId": %d,
                                        "startTime": "2099-12-15T10:00:00",
                                        "endTime": "2099-12-15T11:00:00",
                                        "notes": "Test réservation API"
                                    }
                                    """.formatted(savedResource.getId()))
            )
            .andExpect(status().isCreated());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}