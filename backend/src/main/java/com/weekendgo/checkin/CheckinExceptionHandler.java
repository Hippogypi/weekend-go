package com.weekendgo.checkin;

import com.weekendgo.common.api.ApiResponse;
import com.weekendgo.common.api.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CheckinExceptionHandler {

    @ExceptionHandler(CheckinStorageException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleCheckinStorageException(
            CheckinStorageException exception,
            HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.of(
                "CHECKIN_STORAGE_ERROR",
                "Checkin storage is unavailable",
                request.getRequestURI()
        );
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.fail(error.code(), error.message(), error));
    }
}
