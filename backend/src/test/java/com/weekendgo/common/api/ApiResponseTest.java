package com.weekendgo.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void okWrapsPayloadWithStableSuccessEnvelope() {
        ApiResponse<String> response = ApiResponse.ok("ready");

        assertThat(response.success()).isTrue();
        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.message()).isEqualTo("success");
        assertThat(response.data()).isEqualTo("ready");
    }

    @Test
    void failWrapsErrorPayloadWithStableFailureEnvelope() {
        ErrorResponse error = ErrorResponse.of("BOOTSTRAP_ERROR", "bootstrap failed", "/api/health");
        ApiResponse<ErrorResponse> response = ApiResponse.fail("BOOTSTRAP_ERROR", "bootstrap failed", error);

        assertThat(response.success()).isFalse();
        assertThat(response.code()).isEqualTo("BOOTSTRAP_ERROR");
        assertThat(response.message()).isEqualTo("bootstrap failed");
        assertThat(response.data()).isEqualTo(error);
        assertThat(error.timestamp()).isNotNull();
    }
}
