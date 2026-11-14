package com.microshop.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI tai /v3/api-docs. Swagger UI tap trung o api-gateway: http://localhost:8080/swagger-ui.html
 * Server = "/" (tuong doi) -> "Try it out" goi qua dung host dang mo Swagger (gateway :8080),
 * khong phai dia chi noi bo trong mang docker (vd http://auth-service:9000).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("Auth Service API").version("1.0").description("Profile user va phan quyen (ADMIN). Endpoint OAuth2/OIDC xem /.well-known/openid-configuration."))
                .servers(List.of(new Server().url("/").description("API Gateway (cung origin)")));
    }
}
