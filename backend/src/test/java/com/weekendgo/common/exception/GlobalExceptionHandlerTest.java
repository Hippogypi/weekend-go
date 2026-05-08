package com.weekendgo.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.weekendgo.amap.exception.AmapServiceException;
import com.weekendgo.common.api.ApiResponse;
import com.weekendgo.common.api.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsAmapServiceFailuresToUnifiedBadGatewayResponse() {
        HttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

        ResponseEntity<ApiResponse<ErrorResponse>> response = handler.handleAmapServiceException(
                new AmapServiceException("Amap request failed: INVALID_USER_IP"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().code()).isEqualTo("EXTERNAL_SERVICE_ERROR");
        assertThat(response.getBody().message()).isEqualTo("External map service request failed");
        assertThat(response.getBody().data().path()).isEqualTo("/api/test");
    }
}
