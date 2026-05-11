package com.weekendgo.qa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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

@SpringBootTest(classes = {WeekendGoApplication.class, QaControllerTest.TestQaConfiguration.class})
@AutoConfigureMockMvc
class QaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingQaRepository qaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        qaRepository.clear();
    }

    @Test
    void authenticatedUserCanCreateQuestion() throws Exception {
        String token = registerAndLogin("qa-question-user");

        mockMvc.perform(post("/api/places/42/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Is WiFi free?"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.placeId").value(42))
                .andExpect(jsonPath("$.data.userId").isNumber())
                .andExpect(jsonPath("$.data.type").value("QUESTION"))
                .andExpect(jsonPath("$.data.parentId").doesNotExist())
                .andExpect(jsonPath("$.data.content").value("Is WiFi free?"))
                .andExpect(jsonPath("$.data.answerCount").value(0));

        assertThat(qaRepository.questions).hasSize(1);
        assertThat(qaRepository.questions.get(0).content()).isEqualTo("Is WiFi free?");
    }

    @Test
    void unauthenticatedUserCannotCreateQuestion() throws Exception {
        mockMvc.perform(post("/api/places/42/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Is WiFi free?"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", not("")))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void anyoneCanListQuestions() throws Exception {
        qaRepository.questions.add(new PlaceQa(1, 42, 1, "QUESTION", null, "Q1", 0, Instant.now().minusSeconds(60)));
        qaRepository.questions.add(new PlaceQa(2, 42, 2, "QUESTION", null, "Q2", 2, Instant.now()));
        qaRepository.questions.add(new PlaceQa(3, 99, 3, "QUESTION", null, "Q3", 0, Instant.now()));

        mockMvc.perform(get("/api/places/42/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].content").value("Q2"))
                .andExpect(jsonPath("$.data[1].content").value("Q1"));
    }

    @Test
    void listQuestionsForNonExistentPlaceReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/places/999/questions"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
    }

    @Test
    void authenticatedUserCanCreateAnswer() throws Exception {
        String token = registerAndLogin("qa-answer-user");
        qaRepository.questions.add(new PlaceQa(1, 42, 1, "QUESTION", null, "Q?", 0, Instant.now()));

        mockMvc.perform(post("/api/questions/1/answers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Yes, it is free."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.placeId").value(42))
                .andExpect(jsonPath("$.data.type").value("ANSWER"))
                .andExpect(jsonPath("$.data.parentId").value(1))
                .andExpect(jsonPath("$.data.content").value("Yes, it is free."));

        assertThat(qaRepository.answers).hasSize(1);
    }

    @Test
    void unauthenticatedUserCannotCreateAnswer() throws Exception {
        mockMvc.perform(post("/api/questions/1/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Yes, it is free."
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void anyoneCanListAnswers() throws Exception {
        qaRepository.answers.add(new PlaceQa(1, 42, 2, "ANSWER", 10L, "A1", 0, Instant.now().minusSeconds(60)));
        qaRepository.answers.add(new PlaceQa(2, 42, 3, "ANSWER", 10L, "A2", 0, Instant.now()));
        qaRepository.answers.add(new PlaceQa(3, 42, 4, "ANSWER", 20L, "A3", 0, Instant.now()));

        mockMvc.perform(get("/api/questions/10/answers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].content").value("A1"))
                .andExpect(jsonPath("$.data[1].content").value("A2"));
    }

    @Test
    void createAnswerForNonExistentQuestionReturnsNotFound() throws Exception {
        String token = registerAndLogin("qa-answer-missing");

        mockMvc.perform(post("/api/questions/999/answers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "test"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"));
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
    static class TestQaConfiguration {

        @Bean
        @Primary
        RecordingQaRepository recordingQaRepository() {
            return new RecordingQaRepository();
        }

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

    static class RecordingQaRepository implements QaRepository {

        private final AtomicLong ids = new AtomicLong(1);
        private final List<PlaceQa> questions = new ArrayList<>();
        private final List<PlaceQa> answers = new ArrayList<>();

        @Override
        public PlaceQa createQuestion(long placeId, long userId, QuestionRequest request) {
            PlaceQa question = new PlaceQa(
                    ids.getAndIncrement(),
                    placeId,
                    userId,
                    "QUESTION",
                    null,
                    request.content(),
                    0,
                    Instant.now()
            );
            questions.add(question);
            return question;
        }

        @Override
        public PlaceQa createAnswer(long questionId, long userId, AnswerRequest request) {
            PlaceQa question = questions.stream()
                    .filter(q -> q.id() == questionId)
                    .findFirst()
                    .orElseThrow(QuestionNotFoundException::new);

            PlaceQa answer = new PlaceQa(
                    ids.getAndIncrement(),
                    question.placeId(),
                    userId,
                    "ANSWER",
                    questionId,
                    request.content(),
                    0,
                    Instant.now()
            );
            answers.add(answer);

            int index = questions.indexOf(question);
            questions.set(index, new PlaceQa(
                    question.id(),
                    question.placeId(),
                    question.userId(),
                    question.type(),
                    question.parentId(),
                    question.content(),
                    question.answerCount() + 1,
                    question.createdAt()
            ));
            return answer;
        }

        @Override
        public List<PlaceQa> findQuestionsByPlaceId(long placeId) {
            return questions.stream()
                    .filter(q -> q.placeId() == placeId)
                    .sorted(Comparator.comparing(PlaceQa::createdAt).reversed())
                    .toList();
        }

        @Override
        public List<PlaceQa> findAnswersByQuestionId(long questionId) {
            return answers.stream()
                    .filter(a -> a.parentId() != null && a.parentId() == questionId)
                    .sorted(Comparator.comparing(PlaceQa::createdAt))
                    .toList();
        }

        void clear() {
            questions.clear();
            answers.clear();
            ids.set(1);
        }
    }
}
