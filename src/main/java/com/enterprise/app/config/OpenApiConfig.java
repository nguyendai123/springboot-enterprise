package com.enterprise.app.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Value("${spring.application.name:Enterprise Backend}")
    private String appName;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(appName + " API")
                        .description("""
                                Enterprise Spring Boot Backend — full-stack modern API.
                                
                                Technologies: Java 21 · Spring Boot 3.3 · PostgreSQL · MongoDB · Redis ·
                                Kafka · RabbitMQ · ActiveMQ · gRPC · WebSocket · Resilience4j · Prometheus
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Enterprise Team")
                                .email("dev@enterprise.com")
                                .url("https://enterprise.com"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local"),
                        new Server().url("https://api-staging.enterprise.com").description("Staging"),
                        new Server().url("https://api.enterprise.com").description("Production")
                ));
    }
}