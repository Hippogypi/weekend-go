package com.weekendgo.qa;

import com.weekendgo.auth.AuthenticatedUser;
import com.weekendgo.place.PlaceNotFoundException;
import com.weekendgo.place.PlaceRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class QaService {

    private final QaRepository qaRepository;
    private final PlaceRepository placeRepository;

    public QaService(QaRepository qaRepository, PlaceRepository placeRepository) {
        this.qaRepository = qaRepository;
        this.placeRepository = placeRepository;
    }

    public PlaceQa createQuestion(long placeId, AuthenticatedUser user, QuestionRequest request) {
        ensurePlaceExists(placeId);
        return qaRepository.createQuestion(placeId, user.account().id(), request);
    }

    public PlaceQa createAnswer(long questionId, AuthenticatedUser user, AnswerRequest request) {
        return qaRepository.createAnswer(questionId, user.account().id(), request);
    }

    public List<PlaceQa> questions(long placeId) {
        ensurePlaceExists(placeId);
        return qaRepository.findQuestionsByPlaceId(placeId);
    }

    public List<PlaceQa> answers(long questionId) {
        return qaRepository.findAnswersByQuestionId(questionId);
    }

    private void ensurePlaceExists(long placeId) {
        if (placeRepository.findById(placeId).isEmpty()) {
            throw new PlaceNotFoundException();
        }
    }
}
