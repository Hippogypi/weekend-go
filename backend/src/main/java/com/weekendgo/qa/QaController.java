package com.weekendgo.qa;

import com.weekendgo.auth.AuthenticatedUser;
import com.weekendgo.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QaController {

    private final QaService qaService;

    public QaController(QaService qaService) {
        this.qaService = qaService;
    }

    @PostMapping("/api/places/{placeId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlaceQa> createQuestion(
            @PathVariable long placeId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody QuestionRequest request
    ) {
        return ApiResponse.ok(qaService.createQuestion(placeId, user, request));
    }

    @GetMapping("/api/places/{placeId}/questions")
    public ApiResponse<List<PlaceQa>> questions(@PathVariable long placeId) {
        return ApiResponse.ok(qaService.questions(placeId));
    }

    @PostMapping("/api/questions/{questionId}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlaceQa> createAnswer(
            @PathVariable long questionId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AnswerRequest request
    ) {
        return ApiResponse.ok(qaService.createAnswer(questionId, user, request));
    }

    @GetMapping("/api/questions/{questionId}/answers")
    public ApiResponse<List<PlaceQa>> answers(@PathVariable long questionId) {
        return ApiResponse.ok(qaService.answers(questionId));
    }
}
