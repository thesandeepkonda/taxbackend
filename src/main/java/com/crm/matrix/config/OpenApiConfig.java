package com.crm.matrix.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        final String securitySchemeName = "bearerAuth";

        return new OpenAPI().servers(List.of(new Server()

                .description("Crm Server"))).info(new Info().title("CRM API").version("1.0").description("CRM REST APIs")).addSecurityItem(new SecurityRequirement().addList(securitySchemeName)).components(new Components().addSecuritySchemes(securitySchemeName, new SecurityScheme().name("Authorization").type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}