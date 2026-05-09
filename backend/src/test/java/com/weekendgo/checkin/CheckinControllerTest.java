package com.weekendgo.checkin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weekendgo.WeekendGoApplication;
import com.weekendgo.amap.dto.AmapPoi;
import com.weekendgo.place.Place;
import com.weekendgo.place.PlaceRepository;
import com.weekendgo.place.PlaceSource;
import com.weekendgo.place.WorkspaceStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = {WeekendGoApplication.class, CheckinControllerTest.TestCheckinConfiguration.class})
@AutoConfigureMockMvc
class CheckinControllerTest {

    private static final Instant NOW = Instant.parse("2026-05-09T06:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingCheckinRepository checkinRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        checkinRepository.clear();
    }

    @Test
    void authenticatedUserCanSubmitCheckinFeedback() throws Exception {
        String token = registerAndLogin("checkin-alice");

        mockMvc.perform(post("/api/places/42/checkins")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "crowdLevel": "NORMAL",
                                  "noiseLevel": "RELATIVELY_QUIET",
                                  "hasSeat": true,
                                  "remark": "window seats available"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.placeId").value(42))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.crowdLevel").value("NORMAL"))
                .andExpect(jsonPath("$.data.noiseLevel").value("RELATIVELY_QUIET"))
                .andExpect(jsonPath("$.data.hasSeat").value(true))
                .andExpect(jsonPath("$.data.remark").value("window seats available"));

        assertThat(checkinRepository.saved).hasSize(1);
        assertThat(checkinRepository.saved.get(0).placeId()).isEqualTo(42);
        assertThat(checkinRepository.saved.get(0).createdAt()).isEqualTo(NOW);
    }

    @Test
    void unauthenticatedCheckinReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/places/42/checkins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "crowdLevel": "NORMAL",
                                  "noiseLevel": "NORMAL",
                                  "hasSeat": true
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", not("")))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void currentStatusAggregatesRecentCheckinsWithinTwoHours() throws Exception {
        checkinRepository.saved.add(new SavedCheckin(1, 42, 11, CrowdLevel.CROWDED, NoiseLevel.NOISY, false, null, NOW.minusSeconds(90 * 60)));
        checkinRepository.saved.add(new SavedCheckin(2, 42, 12, CrowdLevel.CROWDED, NoiseLevel.NORMAL, true, null, NOW.minusSeconds(30 * 60)));
        checkinRepository.saved.add(new SavedCheckin(3, 42, 13, CrowdLevel.NORMAL, NoiseLevel.NOISY, false, null, NOW.minusSeconds(10 * 60)));
        checkinRepository.saved.add(new SavedCheckin(4, 42, 14, CrowdLevel.FREE, NoiseLevel.QUIET, true, null, NOW.minusSeconds(3 * 60 * 60)));

        mockMvc.perform(get("/api/places/42/current-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.message").value("近期反馈已聚合"))
                .andExpect(jsonPath("$.data.sampleCount").value(3))
                .andExpect(jsonPath("$.data.crowdLevel").value("CROWDED"))
                .andExpect(jsonPath("$.data.noiseLevel").value("NOISY"))
                .andExpect(jsonPath("$.data.hasSeat").value(false))
                .andExpect(jsonPath("$.data.seatAvailabilityRatio").value(0.33));

        assertThat(checkinRepository.lastCutoff).isEqualTo(NOW.minusSeconds(2 * 60 * 60));
    }

    @Test
    void currentStatusReturnsStableEmptyResponseWhenNoRecentCheckinsExist() throws Exception {
        checkinRepository.saved.add(new SavedCheckin(1, 42, 11, CrowdLevel.FREE, NoiseLevel.QUIET, true, null, NOW.minusSeconds(3 * 60 * 60)));

        mockMvc.perform(get("/api/places/42/current-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("NO_RECENT_FEEDBACK"))
                .andExpect(jsonPath("$.data.message").value("暂无近期反馈"))
                .andExpect(jsonPath("$.data.sampleCount").value(0))
                .andExpect(jsonPath("$.data.crowdLevel").doesNotExist())
                .andExpect(jsonPath("$.data.noiseLevel").doesNotExist())
                .andExpect(jsonPath("$.data.hasSeat").doesNotExist())
                .andExpect(jsonPath("$.data.seatAvailabilityRatio").doesNotExist());
    }

    private String registerAndLogin(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "secret123",
                                  "nickname": "%s"
                                }
                                """.formatted(username, username)))
                .andExpect(status().isCreated());

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
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("token").asText();
    }

    @TestConfiguration
    static class TestCheckinConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }

        @Bean
        @Primary
        RecordingCheckinRepository recordingCheckinRepository() {
            return new RecordingCheckinRepository();
        }

        @Bean
        @Primary
        PlaceRepository recordingPlaceRepository() {
            return new RecordingPlaceRepository();
        }
    }

    static class RecordingCheckinRepository implements CheckinRepository {

        private final AtomicLong ids = new AtomicLong(1);
        private final List<SavedCheckin> saved = new ArrayList<>();
        private Instant lastCutoff;

        @Override
        public SavedCheckin save(NewCheckin checkin) {
            SavedCheckin savedCheckin = new SavedCheckin(
                    ids.getAndIncrement(),
                    checkin.placeId(),
                    checkin.userId(),
                    checkin.crowdLevel(),
                    checkin.noiseLevel(),
                    checkin.hasSeat(),
                    checkin.remark(),
                    checkin.createdAt()
            );
            saved.add(savedCheckin);
            return savedCheckin;
        }

        @Override
        public List<SavedCheckin> findRecentByPlaceId(long placeId, Instant cutoff) {
            lastCutoff = cutoff;
            return saved.stream()
                    .filter(checkin -> checkin.placeId() == placeId)
                    .filter(checkin -> !checkin.createdAt().isBefore(cutoff))
                    .toList();
        }

        void clear() {
            saved.clear();
            ids.set(1);
            lastCutoff = null;
        }
    }

    static class RecordingPlaceRepository implements PlaceRepository {

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
    }
}
