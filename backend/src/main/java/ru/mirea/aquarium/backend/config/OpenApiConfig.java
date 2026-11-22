package ru.mirea.aquarium.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI aquariumApiDescription() {
        return new OpenAPI().components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("bearerAuth", new io.swagger.v3.oas.models.security.SecurityScheme()
                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                        .scheme("bearer").bearerFormat("JWT")))
                .info(new Info()
                .title("Aquarium REST API")
                .version("1.0.0")
                .description("REST API системы учета и обслуживания аквариумов. " +
                        "Модуль рыб требует principal и роли общей авторизации команды; " +
                        "JWT-фильтр подключается отдельно.")
                .contact(new Contact().name("Команда Aquarium")));
    }
}
