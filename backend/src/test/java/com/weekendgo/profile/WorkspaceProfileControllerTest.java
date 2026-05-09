package com.weekendgo.profile;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.weekendgo.WeekendGoApplication;
import com.weekendgo.auth.UserAccountRepository;
import com.weekendgo.auth.UserRole;
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
    void loggedInUserSubmitsPendingProfileContribution() throws Exception {
        String token = createUserAndLogin("alice", UserRole.USER);

        mockMvc.perform(post("/api/places/42/profile-submissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quietScore": 4.5,
                                  "wifiScore": 4.0,
                                  "socketScore": 3.5,
                                  "seatScore": 4.0,
                                  "costScore": 3.0,
                                  "minConsumption": 20,
                                  "allowLongStay": "TRUE",
                                  "suitableScenes": ["READING", "REMOTE_WORK"],
                                  "remark": "stable wifi"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.placeId").value(42))
                .andExpect(jsonPath("$.data.auditStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.quietScore").value(4.5));
    }

    @Test
    void normalUserCannotAuditButAdminCanApproveAndExposeAggregatedProfile() throws Exception {
        String userToken = createUserAndLogin("bob", UserRole.USER);
        String adminToken = createUserAndLogin("root", UserRole.ADMIN);
        long submissionId = submitProfile(userToken);

        mockMvc.perform(post("/api/admin/profile-submissions/%d/approve".formatted(submissionId))
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/profile-submissions/%d/approve".formatted(submissionId))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "useful contribution"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auditStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.auditReason").value("useful contribution"));

        mockMvc.perform(get("/api/places/42/workspace-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.placeId").value(42))
                .andExpect(jsonPath("$.data.quietScore").value(4.0))
                .andExpect(jsonPath("$.data.approvedSubmissionCount").value(1))
                .andExpect(jsonPath("$.data.contributorCount").value(1))
                .andExpect(jsonPath("$.data.trustLevel").value("LOW"));

        mockMvc.perform(get("/api/places/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workspaceProfile.placeId").value(42))
                .andExpect(jsonPath("$.data.workspaceProfile.approvedSubmissionCount").value(1));
    }

    @Test
    void adminCanRejectPendingContributionWithoutUpdatingPublicProfile() throws Exception {
        String userToken = createUserAndLogin("cathy", UserRole.USER);
        String adminToken = createUserAndLogin("admin2", UserRole.ADMIN);
        long submissionId = submitProfile(userToken);

        mockMvc.perform(post("/api/admin/profile-submissions/%d/reject".formatted(submissionId))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "insufficient detail"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auditStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.auditReason").value("insufficient detail"));

        mockMvc.perform(get("/api/places/42/workspace-profile"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORKSPACE_PROFILE_NOT_FOUND"));
    }

    private long submitProfile(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/places/42/profile-submissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quietScore": 4.0,
                                  "wifiScore": 4.0,
                                  "socketScore": 5.0,
                                  "seatScore": 3.0,
                                  "costScore": 4.0,
                                  "allowLongStay": "TRUE",
                                  "remark": "works well"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
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
        PlaceRepository profilePlaceRepository() {
            return new FixedPlaceRepository();
        }

        @Bean
        @Primary
        WorkspaceProfileRepository workspaceProfileRepository() {
            return new InMemoryWorkspaceProfileRepository();
        }
    }

    static class FixedPlaceRepository implements PlaceRepository {

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
    }
}
