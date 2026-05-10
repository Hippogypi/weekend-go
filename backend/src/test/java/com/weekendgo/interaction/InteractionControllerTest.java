package com.weekendgo.interaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weekendgo.WeekendGoApplication;
import com.weekendgo.amap.dto.AmapPoi;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = {WeekendGoApplication.class, InteractionControllerTest.TestPlaceConfiguration.class})
@AutoConfigureMockMvc
class InteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void submitReviewRequiresLoginAndApprovedReviewsArePublic() throws Exception {
        mockMvc.perform(post("/api/places/42/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson("quiet before approval")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        String userToken = registerAndLogin("reviewer", UserRole.USER);
        MvcResult created = mockMvc.perform(post("/api/places/42/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson("quiet before approval")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.auditStatus").value("PENDING"))
                .andReturn();
        long reviewId = readLong(created, "data", "id");

        mockMvc.perform(get("/api/places/42/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        String adminToken = registerAndLogin("review-admin", UserRole.ADMIN);
        mockMvc.perform(patch("/api/admin/reviews/{reviewId}/audit", reviewId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "auditStatus": "APPROVED",
                                  "reason": "ok"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.auditStatus").value("APPROVED"));

        mockMvc.perform(get("/api/places/42/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].content").value("quiet before approval"))
                .andExpect(jsonPath("$.data[0].auditStatus").doesNotExist());
    }

    @Test
    void userCanFavoriteUnfavoriteAndListPlaces() throws Exception {
        String userToken = registerAndLogin("favorite-user", UserRole.USER);

        mockMvc.perform(get("/api/places/42/favorite")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(false));

        mockMvc.perform(post("/api/places/42/favorite")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(true));

        mockMvc.perform(get("/api/me/favorites")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].placeId").value(42))
                .andExpect(jsonPath("$.data[0].placeName").value("City Library"));

        mockMvc.perform(delete("/api/places/42/favorite")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(false));

        mockMvc.perform(get("/api/places/42/favorite")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(false));
    }

    @Test
    void submitImageRequiresLoginAndApprovedImagesArePublic() throws Exception {
        mockMvc.perform(post("/api/places/42/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageUrl": "https://example.com/library.jpg",
                                  "description": "reading area"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        String userToken = registerAndLogin("image-user", UserRole.USER);
        MvcResult created = mockMvc.perform(post("/api/places/42/images")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imageUrl": "https://example.com/library.jpg",
                                  "description": "reading area"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.auditStatus").value("PENDING"))
                .andReturn();
        long imageId = readLong(created, "data", "id");

        mockMvc.perform(get("/api/places/42/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));

        String regularToken = registerAndLogin("regular-user", UserRole.USER);
        mockMvc.perform(patch("/api/admin/images/{imageId}/audit", imageId)
                        .header("Authorization", "Bearer " + regularToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "auditStatus": "APPROVED"
                                }
                                """))
                .andExpect(status().isForbidden());

        String adminToken = registerAndLogin("image-admin", UserRole.ADMIN);
        mockMvc.perform(patch("/api/admin/images/{imageId}/audit", imageId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "auditStatus": "APPROVED"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/places/42/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].imageUrl").value("https://example.com/library.jpg"))
                .andExpect(jsonPath("$.data[0].auditStatus").doesNotExist());
    }

    @Test
    void authenticatedUserCanListOwnReviews() throws Exception {
        String userToken = registerAndLogin("review-lister", UserRole.USER);

        mockMvc.perform(post("/api/places/42/reviews")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson("my own review")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/me/reviews")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].content").value("my own review"))
                .andExpect(jsonPath("$.data[0].auditStatus").value("PENDING"))
                .andExpect(jsonPath("$.data[0].placeName").value("City Library"));
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
        assertThat(current.isTextual()).isTrue();
        return current.asText();
    }

    private long readLong(MvcResult result, String... path) throws Exception {
        JsonNode current = read(result, path);
        assertThat(current.isNumber()).isTrue();
        return current.asLong();
    }

    private JsonNode read(MvcResult result, String... path) throws Exception {
        JsonNode current = objectMapper.readTree(result.getResponse().getContentAsString());
        for (String segment : path) {
            current = current.path(segment);
        }
        assertThat(current.isMissingNode()).isFalse();
        return current;
    }

    private String reviewJson(String content) {
        return """
                {
                  "quietScore": 4.5,
                  "wifiScore": 4.0,
                  "socketScore": 5.0,
                  "comfortScore": 4.0,
                  "costScore": 3.5,
                  "content": "%s"
                }
                """.formatted(content);
    }

    @TestConfiguration
    static class TestPlaceConfiguration {

        @Bean
        @Primary
        PlaceRepository testPlaceRepository() {
            return new PlaceRepository() {
                @Override
                public List<Place> saveAllFromAmap(List<AmapPoi> pois) {
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
    }
}
