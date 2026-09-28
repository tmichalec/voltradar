package sk.brutech.voltradar.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Logs OpenAPI and Swagger UI access URLs upon successful application startup.
 */
@Component
public class OpenApiStartupLogger {
    private static final Logger log = LoggerFactory.getLogger(OpenApiStartupLogger.class);

    private final int serverPort;
    private final String contextPath;
    private final String swaggerPath;
    private final boolean swaggerEnabled;
    private final String apiDocsPath;
    private final boolean apiDocsEnabled;

    public OpenApiStartupLogger(
            @Value("${server.port:8080}") int serverPort,
            @Value("${server.servlet.context-path:}") String contextPath,
            @Value("${springdoc.swagger-ui.path:/swagger-ui.html}") String swaggerPath,
            @Value("${springdoc.swagger-ui.enabled:true}") boolean swaggerEnabled,
            @Value("${springdoc.api-docs.path:/v3/api-docs}") String apiDocsPath,
            @Value("${springdoc.api-docs.enabled:true}") boolean apiDocsEnabled
    ) {
        this.serverPort = serverPort;
        this.contextPath = contextPath != null ? contextPath : "";
        this.swaggerPath = swaggerPath != null ? swaggerPath : "/swagger-ui.html";
        this.swaggerEnabled = swaggerEnabled;
        this.apiDocsPath = apiDocsPath != null ? apiDocsPath : "/v3/api-docs";
        this.apiDocsEnabled = apiDocsEnabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!swaggerEnabled && !apiDocsEnabled) {
            return;
        }

        String baseUrl = "http://localhost:" + serverPort + contextPath;
        String swaggerUrl = baseUrl + (swaggerPath.startsWith("/") ? swaggerPath : "/" + swaggerPath);
        String apiDocsUrl = baseUrl + (apiDocsPath.startsWith("/") ? apiDocsPath : "/" + apiDocsPath);

        StringBuilder sb = new StringBuilder();
        sb.append("\n--------------------------------------------------------------------------------\n");
        sb.append("  VoltRadar API Documentation is ready!\n");
        if (swaggerEnabled) {
            sb.append("  Swagger UI:   ").append(swaggerUrl).append("\n");
        }
        if (apiDocsEnabled) {
            sb.append("  OpenAPI JSON: ").append(apiDocsUrl).append("\n");
        }
        sb.append("--------------------------------------------------------------------------------");

        log.info("{}", sb);
    }
}
