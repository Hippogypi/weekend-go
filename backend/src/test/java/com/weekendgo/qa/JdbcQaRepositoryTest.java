package com.weekendgo.qa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

class JdbcQaRepositoryTest {

    private static final String JDBC_URL = "jdbc:h2:mem:qa;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    private JdbcTemplate jdbcTemplate;
    private JdbcQaRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(JDBC_URL);
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcQaRepository(
                jdbcTemplate,
                new TransactionTemplate(new DataSourceTransactionManager(dataSource))
        );

        jdbcTemplate.execute("DROP TABLE IF EXISTS place_qa");
        jdbcTemplate.execute("""
                CREATE TABLE place_qa (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  place_id BIGINT NOT NULL,
                  user_id BIGINT NOT NULL,
                  type VARCHAR(16) NOT NULL,
                  parent_id BIGINT,
                  content VARCHAR(1000) NOT NULL,
                  answer_count INT NOT NULL DEFAULT 0,
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }

    @Test
    void createQuestionPersistsAndReturnsQuestion() {
        PlaceQa question = repository.createQuestion(42, 1, new QuestionRequest("Is WiFi free?"));

        assertThat(question.id()).isPositive();
        assertThat(question.placeId()).isEqualTo(42);
        assertThat(question.userId()).isEqualTo(1);
        assertThat(question.type()).isEqualTo("QUESTION");
        assertThat(question.parentId()).isNull();
        assertThat(question.content()).isEqualTo("Is WiFi free?");
        assertThat(question.answerCount()).isEqualTo(0);
        assertThat(question.createdAt()).isNotNull();
    }

    @Test
    void createAnswerPersistsAndReturnsAnswerAndIncrementsCount() {
        PlaceQa question = repository.createQuestion(42, 1, new QuestionRequest("Is WiFi free?"));

        PlaceQa answer = repository.createAnswer(question.id(), 2, new AnswerRequest("Yes, and it's fast."));

        assertThat(answer.id()).isPositive();
        assertThat(answer.placeId()).isEqualTo(42);
        assertThat(answer.userId()).isEqualTo(2);
        assertThat(answer.type()).isEqualTo("ANSWER");
        assertThat(answer.parentId()).isEqualTo(question.id());
        assertThat(answer.content()).isEqualTo("Yes, and it's fast.");

        PlaceQa updatedQuestion = repository.findQuestionsByPlaceId(42).get(0);
        assertThat(updatedQuestion.answerCount()).isEqualTo(1);
    }

    @Test
    void createAnswerForNonExistentQuestionThrowsQuestionNotFoundException() {
        assertThatThrownBy(() -> repository.createAnswer(999, 1, new AnswerRequest("test")))
                .isInstanceOf(QuestionNotFoundException.class);
    }

    @Test
    void findQuestionsByPlaceIdReturnsQuestionsInDescendingOrder() {
        repository.createQuestion(42, 1, new QuestionRequest("Q1"));
        repository.createQuestion(42, 2, new QuestionRequest("Q2"));
        repository.createQuestion(99, 3, new QuestionRequest("Q3"));

        List<PlaceQa> questions = repository.findQuestionsByPlaceId(42);

        assertThat(questions).hasSize(2);
        assertThat(questions).extracting(PlaceQa::content).containsExactly("Q2", "Q1");
    }

    @Test
    void findAnswersByQuestionIdReturnsAnswersInAscendingOrder() {
        PlaceQa question = repository.createQuestion(42, 1, new QuestionRequest("Q?"));
        repository.createAnswer(question.id(), 2, new AnswerRequest("A1"));
        repository.createAnswer(question.id(), 3, new AnswerRequest("A2"));

        List<PlaceQa> answers = repository.findAnswersByQuestionId(question.id());

        assertThat(answers).hasSize(2);
        assertThat(answers).extracting(PlaceQa::content).containsExactly("A1", "A2");
    }

    @Test
    void findAnswersByQuestionIdReturnsEmptyListWhenNoAnswers() {
        PlaceQa question = repository.createQuestion(42, 1, new QuestionRequest("Q?"));

        List<PlaceQa> answers = repository.findAnswersByQuestionId(question.id());

        assertThat(answers).isEmpty();
    }
}
