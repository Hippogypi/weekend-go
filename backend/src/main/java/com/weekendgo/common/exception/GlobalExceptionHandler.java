package com.weekendgo.common.exception;

import com.weekendgo.common.api.ApiResponse;
import com.weekendgo.common.api.ErrorResponse;
import com.weekendgo.auth.DuplicateUsernameException;
import com.weekendgo.auth.InvalidCredentialsException;
import com.weekendgo.amap.exception.AmapServiceException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleException(
            Exception exception,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                "INTERNAL_SERVER_ERROR",
                "Unexpected server error",
                request.getRequestURI()
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(error.code(), error.message(), error));
    }
}
