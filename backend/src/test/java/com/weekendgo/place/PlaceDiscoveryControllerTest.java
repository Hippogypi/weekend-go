package com.weekendgo.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weekendgo.WeekendGoApplication;
import com.weekendgo.amap.AmapService;
import com.weekendgo.amap.dto.AmapPoi;
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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = {WeekendGoApplication.class, PlaceDiscoveryControllerTest.TestPlaceRepositoryConfiguration.class})
@AutoConfigureMockMvc
class PlaceDiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingPlaceRepository placeRepository;

    @Autowired
    private RecordingAmapService amapService;

    @Test
    void searchIsPublicAndPersistsAmapPoisWithLinks() throws Exception {
        amapService.keywordResults = List.of(new AmapPoi(
                        "B0LIBRARY",
                        "City Library",
                        "Science/Culture",
                        "College Road",
                        "116.300000,39.900000",
                        "Haidian"
                ));

        mockMvc.perform(get("/api/workspaces/search")
                        .param("keyword", "library")
                        .param("city", "Beijing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(42))
                .andExpect(jsonPath("$.data[0].amapPoiId").value("B0LIBRARY"))
                .andExpect(jsonPath("$.data[0].name").value("City Library"))
                .andExpect(jsonPath("$.data[0].longitude").value(116.3))
                .andExpect(jsonPath("$.data[0].latitude").value(39.9))
                .andExpect(jsonPath("$.data[0].links.detail").value("/api/places/42"))
                .andExpect(jsonPath("$.data[0].links.profileContributions").value("/api/places/42/profile-submissions"))
                .andExpect(jsonPath("$.data[0].links.checkins").value("/api/places/42/checkins"))
                .andExpect(jsonPath("$.data[0].links.reviews").value("/api/places/42/reviews"))
                .andExpect(jsonPath("$.data[0].links.images").value("/api/places/42/images"));

        assertThat(amapService.lastKeywordSearch).isEqualTo(new KeywordSearch("library", "Beijing", 1, 20));
        assertThat(placeRepository.lastSaved).extracting(AmapPoi::id).containsExactly("B0LIBRARY");
    }

    @Test
    void nearbySearchUsesAmapAroundSearchAndPersistsResults() throws Exception {
        amapService.aroundResults = List.of(new AmapPoi(
                        "B0CAFE",
                        "Office Coffee",
                        "Food/Beverage",
                        "Wangjing Street",
                        "116.481488,39.990464",
                        "Chaoyang"
                ));

        mockMvc.perform(get("/api/workspaces/nearby")
                        .param("longitude", "116.481488")
                        .param("latitude", "39.990464")
                        .param("keyword", "cafe")
                        .param("radius", "800")
                        .param("page", "2")
                        .param("offset", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(43))
                .andExpect(jsonPath("$.data[0].name").value("Office Coffee"));

        assertThat(amapService.lastAroundSearch)
                .isEqualTo(new AroundSearch("116.481488,39.990464", "cafe", 800, 2, 10));
        assertThat(placeRepository.lastSaved).extracting(AmapPoi::id).containsExactly("B0CAFE");
    }

    @Test
    void placeDetailReturnsPersistedPlaceWithRelatedResourceLinks() throws Exception {
        mockMvc.perform(get("/api/places/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.amapPoiId").value("B0LIBRARY"))
                .andExpect(jsonPath("$.data.workspaceStatus").value("CANDIDATE"))
                .andExpect(jsonPath("$.data.links.checkins").value("/api/places/42/checkins"));
    }

    @Test
    void placeWriteLikeRoutesRemainProtectedWhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/places/42/checkins"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @TestConfiguration
    static class TestPlaceRepositoryConfiguration {

        @Bean
        @Primary
        RecordingPlaceRepository recordingPlaceRepository() {
            return new RecordingPlaceRepository();
        }

        @Bean
        @Primary
        RecordingAmapService recordingAmapService() {
            return new RecordingAmapService();
        }
    }

    static class RecordingAmapService extends AmapService {

        private List<AmapPoi> keywordResults = List.of();
        private List<AmapPoi> aroundResults = List.of();
        private KeywordSearch lastKeywordSearch;
        private AroundSearch lastAroundSearch;

        RecordingAmapService() {
            super(null);
        }

        @Override
        public List<AmapPoi> searchAround(String location, String keywords, int radius, int page, int offset) {
            lastAroundSearch = new AroundSearch(location, keywords, radius, page, offset);
            return aroundResults;
        }

        @Override
        public List<AmapPoi> searchByKeyword(String keywords, String city, int page, int offset) {
            lastKeywordSearch = new KeywordSearch(keywords, city, page, offset);
            return keywordResults;
        }
    }

    record KeywordSearch(String keyword, String city, int page, int offset) {
    }

    record AroundSearch(String location, String keyword, int radius, int page, int offset) {
    }

    static class RecordingPlaceRepository implements PlaceRepository {

        private List<AmapPoi> lastSaved = List.of();

        @Override
        public List<Place> saveAllFromAmap(List<AmapPoi> pois) {
            lastSaved = List.copyOf(pois);
            return pois.stream()
                    .map(this::toPlace)
                    .toList();
        }

        @Override
        public Optional<Place> findById(long id) {
            if (id == 42) {
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
            return Optional.empty();
        }

        private Place toPlace(AmapPoi poi) {
            long id = "B0CAFE".equals(poi.id()) ? 43 : 42;
            String[] coordinates = poi.location().split(",");
            return new Place(
                    id,
                    poi.id(),
                    poi.name(),
                    poi.address(),
                    new BigDecimal(coordinates[0]),
                    new BigDecimal(coordinates[1]),
                    poi.type(),
                    null,
                    null,
                    null,
                    poi.district(),
                    PlaceSource.AMAP_SEARCH,
                    WorkspaceStatus.CANDIDATE
            );
        }
    }
}
