package com.weekendgo.qa;

import java.util.List;

public interface QaRepository {

    PlaceQa createQuestion(long placeId, long userId, QuestionRequest request);

    PlaceQa createAnswer(long questionId, long userId, AnswerRequest request);

    List<PlaceQa> findQuestionsByPlaceId(long placeId);

    List<PlaceQa> findAnswersByQuestionId(long questionId);
}
