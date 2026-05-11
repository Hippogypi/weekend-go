package com.weekendgo.profile;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = {WeekendGoApplication.class, WorkspaceProfileControllerTest.TestConfigurationBeans.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WorkspaceProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void publicProfileAggregatesFromApprovedReviews() throws Exception {
        String userToken = createUserAndLogin("alice", UserRole.USER);
        String adminToken = createUserAndLogin("root", UserRole.ADMIN);

        MvcResult created = mockMvc.perform(post("/api/places/42/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quietScore": 4.0,
                                  "wifiScore": 4.0,
                                  "socketScore": 5.0,
                                  "comfortScore": 4.0,
                                  "costScore": 3.0,
                                  "content": "nice place",
                                  "seatScore": 4.0,
                                  "minConsumption": 20,
                                  "allowLongStay": "TRUE",
                                  "suitableScenes": ["READING", "REMOTE_WORK"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Number reviewIdNum = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");
        long reviewId = reviewIdNum.longValue();

        mockMvc.perform(patch("/api/admin/reviews/{reviewId}/audit", reviewId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "auditStatus": "APPROVED",
                                  "reason": "ok"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/places/42/workspace-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.placeId").value(42))
                .andExpect(jsonPath("$.data.quietScore").value(4.0))
                .andExpect(jsonPath("$.data.wifiScore").value(4.0))
                .andExpect(jsonPath("$.data.socketScore").value(5.0))
                .andExpect(jsonPath("$.data.seatScore").value(4.0))
                .andExpect(jsonPath("$.data.costScore").value(3.0))
                .andExpect(jsonPath("$.data.minConsumption").value(20))
                .andExpect(jsonPath("$.data.allowLongStay").value("TRUE"))
                .andExpect(jsonPath("$.data.approvedSubmissionCount").value(1))
                .andExpect(jsonPath("$.data.contributorCount").value(1))
                .andExpect(jsonPath("$.data.trustLevel").value("LOW"));
    }

    @Test
    void publicProfileReturnsNotFoundWhenNoApprovedReviews() throws Exception {
        mockMvc.perform(get("/api/places/42/workspace-profile"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORKSPACE_PROFILE_NOT_FOUND"));
    }

    private String createUserAndLogin(String username, UserRole role) throws Exception {
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
                .andExpect(jsonPath("$.data.token", startsWith("wg_")))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    @TestConfiguration
    static class TestConfigurationBeans {

        @Bean
        @Primary
        InteractionRepository interactionRepository() {
            return new InMemoryInteractionRepository();
        }

        @Bean
        @Primary
        PlaceRepository profilePlaceRepository() {
            return new FixedPlaceRepository();
        }

        @Bean
        @Primary
        WorkspaceProfileRepository workspaceProfileRepository(InteractionRepository interactionRepository) {
            return new InMemoryWorkspaceProfileRepository(interactionRepository);
        }
    }

    static class FixedPlaceRepository implements PlaceRepository {

        @Override
        public java.util.List<Place> saveAllFromAmap(java.util.List<com.weekendgo.amap.dto.AmapPoi> pois) {
            return java.util.List.of();
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
    }
}
