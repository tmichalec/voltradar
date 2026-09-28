package sk.brutech.voltradar.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class OpenApiStartupLoggerTest {

    @Test
    void logsWhenEnabled() {
        OpenApiStartupLogger logger = new OpenApiStartupLogger(
                8080,
                "",
                "/swagger-ui.html",
                true,
                "/v3/api-docs",
                true
        );

        assertDoesNotThrow(logger::onApplicationReady);
    }

    @Test
    void handlesDisabledAndCustomContextPath() {
        OpenApiStartupLogger logger = new OpenApiStartupLogger(
                9090,
                "/api",
                "custom-swagger",
                false,
                "custom-docs",
                false
        );

        assertDoesNotThrow(logger::onApplicationReady);
    }
}
