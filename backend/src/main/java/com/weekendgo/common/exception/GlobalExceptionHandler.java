package com.weekendgo.common.exception;

import com.weekendgo.common.api.ApiResponse;
import com.weekendgo.common.api.ErrorResponse;
import com.weekendgo.auth.DuplicateUsernameException;
import com.weekendgo.auth.InvalidCredentialsException;
import com.weekendgo.amap.exception.AmapServiceException;
import com.weekendgo.interaction.InteractionStorageException;
import com.weekendgo.place.PlaceNotFoundException;
import com.weekendgo.place.PlaceStorageException;
import com.weekendgo.profile.ProfileStorageException;
import com.weekendgo.profile.WorkspaceProfileNotFoundException;
import com.weekendgo.qa.QaStorageException;
import com.weekendgo.qa.QuestionNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                "VALIDATION_ERROR",
                "Request validation failed",
                request.getRequestURI()
        );
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(error.code(), error.message(), error));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                "VALIDATION_ERROR",
                "Request validation failed",
                request.getRequestURI()
        );
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(error.code(), error.message(), error));
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleDuplicateUsername(
            DuplicateUsernameException exception,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                "USERNAME_ALREADY_EXISTS",
                "Username already exists",
                request.getRequestURI()
        );
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(error.code(), error.message(), error));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                "UNAUTHORIZED",
                "Invalid username or password",
                request.getRequestURI()
        );
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(error.code(), error.message(), error));
    }

    @ExceptionHandler(AmapServiceException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAmapServiceException(
            AmapServiceException exception,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                "EXTERNAL_SERVICE_ERROR",
                "External map service request failed",
                request.getRequestURI()
        );
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.fail(error.code(), error.message(), error));
    }

    @ExceptionHandler(PlaceNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handlePlaceNotFound(
            PlaceNotFoundException exception,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                "PLACE_NOT_FOUND",
                "Place not found",
                request.getRequestURI()
        );
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(error.code(), error.message(), error));
    }

    @ExceptionHandler(PlaceStorageException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handlePlaceStorageException(
            PlaceStorageException exception,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                "PLACE_STORAGE_ERROR",
                "Place storage is unavailable",
                request.getRequestURI()
        );
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.fail(error.code(), error.message(), error));
    }

    @ExceptionHandler(InteractionStorageException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleInteractionStorageException(
            InteractionStorageException exception,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                "INTERACTION_STORAGE_ERROR",
                "Interaction storage is unavailable",
                request.getRequestURI()
        );
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.fail(error.code(), error.message(), error));
    }

    @ExceptionHandler(WorkspaceProfileNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleWorkspaceProfileNotFound(
            WorkspaceProfileNotFoundException exception,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                "WORKSPACE_PROFILE_NOT_FOUND",
                "Workspace profile not found",
                request.getRequestURI()
        );
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(error.code(), error.message(), error));
    }

    @ExceptionHandler(ProfileStorageException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleProfileStorageException(
            ProfileStorageException exception,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                "PROFILE_STORAGE_ERROR",
                "Workspace profile storage is unavailable",
                request.getRequestURI()
        );
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.fail(error.code(), error.message(), error));
    }

    @ExceptionHandler(QuestionNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleQuestionNotFound(
            QuestionNotFoundException exception,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                "QUESTION_NOT_FOUND",
                "Question not found",
                request.getRequestURI()
        );
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(error.code(), error.message(), error));
    }

    @ExceptionHandler(QaStorageException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleQaStorageException(
            QaStorageException exception,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                "QA_STORAGE_ERROR",
                "Qa storage is unavailable",
                request.getRequestURI()
        );
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.fail(error.code(), error.message(), error));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        ErrorResponse error = ErrorResponse.of(
                status.name(),
                exception.getReason() == null ? status.getReasonPhrase() : exception.getReason(),
                request.getRequestURI()
        );
        return ResponseEntity
                .status(status)
                .body(ApiResponse.fail(error.code(), error.message(), error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleException(
            Exception exception,
            HttpServletRequest request
    ) {
        exception.printStackTrace();
        ErrorResponse error = ErrorResponse.of(
                "INTERNAL_SERVER_ERROR",
                "Unexpected server error: " + exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(error.code(), error.message(), error));
    }
}
