package com.weekendgo.mapmarker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weekendgo.WeekendGoApplication;
import com.weekendgo.auth.UserAccount;
import com.weekendgo.auth.UserAccountRepository;
import com.weekendgo.auth.UserRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = {WeekendGoApplication.class, MapMarkerControllerTest.TestMapMarkerRepositoryConfiguration.class})
@AutoConfigureMockMvc
class MapMarkerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void markersEndpointIsPublicAndReturnsData() throws Exception {
        mockMvc.perform(get("/api/map/markers")
                        .param("longitude", "116.4")
                        .param("latitude", "39.9")
                        .param("radius", "3000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Marked Place"))
                .andExpect(jsonPath("$.data[0].marked").value(true))
                .andExpect(jsonPath("$.data[0].favorited").value(false));
    }

    @Test
    void anonymousUserDoesNotSeeFavorites() throws Exception {
        mockMvc.perform(get("/api/map/markers")
                        .param("longitude", "116.4")
                        .param("latitude", "39.9")
                        .param("radius", "3000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].favorited").value(everyItem(is(false))));
    }

    @Test
    void authenticatedUserSeesFavorites() throws Exception {
        String token = registerAndLogin("marker-user", UserRole.USER);

        mockMvc.perform(get("/api/map/markers")
                        .param("longitude", "116.4")
                        .param("latitude", "39.9")
                        .param("radius", "3000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == 2)].favorited").value(true));
    }

    @Test
    void invalidRadiusReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/map/markers")
                        .param("longitude", "116.4")
                        .param("latitude", "39.9")
                        .param("radius", "0"))
                .andExpect(status().isBadRequest());
    }

    private String registerAndLogin(String username, UserRole role) throws Exception {
        userAccountRepository.save(username, passwordEncoder.encode("secret123"), role, username);
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "secret123"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
        assertThat(node.path("success").asBoolean()).isTrue();
        return node.path("data").path("token").asText();
    }

    @TestConfiguration
    static class TestMapMarkerRepositoryConfiguration {

        @Bean
        @Primary
        MapMarkerRepository testMapMarkerRepository() {
            return new MapMarkerRepository() {
                @Override
                public List<MapMarkerResponse> findNearbyMarkers(BigDecimal longitude, BigDecimal latitude, double radiusMeters, Long userId) {
                    List<MapMarkerResponse> all = List.of(
                            new MapMarkerResponse(1, "Marked Place", longitude, latitude, "Road 1", true, false),
                            new MapMarkerResponse(2, "Favorite Place", longitude.add(new BigDecimal("0.001")), latitude.add(new BigDecimal("0.001")), "Road 2", false, true),
                            new MapMarkerResponse(3, "Both Place", longitude.add(new BigDecimal("0.002")), latitude.add(new BigDecimal("0.002")), "Road 3", true, true)
                    );
                    if (userId == null) {
                        return all.stream()
                                .filter(m -> m.marked())
                                .map(m -> new MapMarkerResponse(m.id(), m.name(), m.longitude(), m.latitude(), m.address(), m.marked(), false))
                                .toList();
                    }
                    return all;
                }
            };
        }
    }
}
