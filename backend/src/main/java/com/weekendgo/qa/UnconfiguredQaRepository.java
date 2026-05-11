package com.weekendgo.qa;

import java.util.List;

public class UnconfiguredQaRepository implements QaRepository {

    @Override
    public PlaceQa createQuestion(long placeId, long userId, QuestionRequest request) {
        throw new QaStorageException("Qa storage is unavailable");
    }

    @Override
    public PlaceQa createAnswer(long questionId, long userId, AnswerRequest request) {
        throw new QaStorageException("Qa storage is unavailable");
    }

    @Override
    public List<PlaceQa> findQuestionsByPlaceId(long placeId) {
        return List.of();
    }

    @Override
    public List<PlaceQa> findAnswersByQuestionId(long questionId) {
        return List.of();
    }
}
