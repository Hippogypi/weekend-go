package com.weekendgo.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weekendgo.WeekendGoApplication;
import com.weekendgo.auth.UserAccountRepository;
import com.weekendgo.auth.UserRole;
import com.weekendgo.interaction.InMemoryInteractionRepository;
import com.weekendgo.interaction.InteractionRepository;
import com.weekendgo.place.Place;
import com.weekendgo.place.PlaceRepository;
import com.weekendgo.place.PlaceSource;
import com.weekendgo.place.WorkspaceStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = {WeekendGoApplication.class, AdminControllerTest.TestPlaceConfiguration.class})
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void pendingListAndStatsRequireAdmin() throws Exception {
        String regularToken = registerAndLogin("regular-auditor", UserRole.USER);

        mockMvc.perform(get("/api/admin/audits/pending-list?type=review")
                        .header("Authorization", "Bearer " + regularToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/admin/audits/stats")
                        .header("Authorization", "Bearer " + regularToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCanRetrievePendingListAndStats() throws Exception {
        String adminToken = registerAndLogin("admin-dashboard", UserRole.ADMIN);

        mockMvc.perform(get("/api/admin/audits/pending-list?type=review&page=1&size=10")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10));

        mockMvc.perform(get("/api/admin/audits/pending-list?type=image")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/admin/audits/stats")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingReviews").value(0))
                .andExpect(jsonPath("$.data.pendingImages").value(0))
                .andExpect(jsonPath("$.data.todayApproved").value(0))
                .andExpect(jsonPath("$.data.todayRejected").value(0));
    }

    @Test
    void invalidTypeReturnsBadRequest() throws Exception {
        String adminToken = registerAndLogin("admin-invalid", UserRole.ADMIN);

        mockMvc.perform(get("/api/admin/audits/pending-list?type=unknown")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    private String registerAndLogin(String username, UserRole role) throws Exception {
        userAccountRepository.save(username, passwordEncoder.encode("secret123"), role, username);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "secret123"
                                }
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn();
        return readString(result, "data", "token");
    }

    private String readString(MvcResult result, String... path) throws Exception {
        JsonNode current = read(result, path);
        return current.asText();
    }

    private JsonNode read(MvcResult result, String... path) throws Exception {
        JsonNode current = objectMapper.readTree(result.getResponse().getContentAsString());
        for (String segment : path) {
            current = current.path(segment);
        }
        return current;
    }

    @TestConfiguration
    static class TestPlaceConfiguration {

        @Bean
        @Primary
        PlaceRepository testPlaceRepository() {
            return new PlaceRepository() {
                @Override
                public List<Place> saveAllFromAmap(List<com.weekendgo.amap.dto.AmapPoi> pois) {
                    return List.of();
                }

                @Override
                public Optional<Place> findById(long id) {
                    if (id != 42) {
                        return Optional.empty();
                    }
                    return Optional.of(new Place(
                            42,
                            "B0LIBRARY",
                            "City Library",
                            "College Road",
                            new BigDecimal("116.300000"),
                            new BigDecimal("39.900000"),
                            "Science/Culture",
                            null,
                            null,
                            null,
                            "Haidian",
                            PlaceSource.AMAP_SEARCH,
                            WorkspaceStatus.CANDIDATE
                    ));
                }
            };
        }

        @Bean
        @Primary
        InteractionRepository testInteractionRepository() {
            return new InMemoryInteractionRepository();
        }
    }
}
