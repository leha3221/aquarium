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
        return new OpenAPI().info(new Info()
                .title("Aquarium REST API")
                .version("1.0.0")
                .description("Открытый REST API системы учета и обслуживания аквариумов. " +
                        "Авторизация не используется; запросы доступны без токена.")
                .contact(new Contact().name("Команда Aquarium")));
    }
}
