package com.boot.ict05_final_user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ICT05 User API")
                        .version("v1.0")
                        .description("가맹점 시스템 관리")
                        .contact(new Contact()
                                .name("ICT Team")
                                .email("ict@example.com")
                                .url("https://example.com"))
                );
    }
}
