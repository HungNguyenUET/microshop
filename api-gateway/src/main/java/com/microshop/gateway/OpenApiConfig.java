package com.microshop.gateway;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger UI chung tai http://localhost:8080/swagger-ui.html.
 * Dropdown liet ke tai lieu cua tung service (route /v3/api-docs/{service} trong application.yml),
 * bean nay chi mo ta API rieng cua gateway (/api/me).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("API Gateway (BFF)").version("1.0")
                        .description("Thong tin user dang dang nhap. Cac API nghiep vu: chon service o dropdown phia tren."))
                .servers(List.of(new Server().url("/").description("API Gateway (cung origin)")));
    }
}
