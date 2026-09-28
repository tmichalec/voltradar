package sk.brutech.voltradar.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) configuration for VoltRadar REST APIs.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI voltRadarOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("VoltRadar API")
                        .description("EV Charging Discovery & Personalized Cost Optimization Platform")
                        .version("0.1.0-SNAPSHOT")
                        .contact(new Contact()
                                .name("VoltRadar Team")
                                .url("https://github.com/brutech/voltradar"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
