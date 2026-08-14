package com.ecom.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;


@Configuration


// he global API details (title, version, description, servers).
@OpenAPIDefinition(
        info = @Info(
                title = "E-Commerce Platform API",
                version = "1.0.0",
                description = """
            Complete backend API for a B2B/B2C ecom platform.

            Built with Spring Boot 3, MongoDB, Redis, Kafka, and WebSockets.

            ## Authentication
            Most endpoints require a JWT access token. Obtain one via
            POST /api/auth/login or POST /api/auth/register, then click
            the "Authorize" button below and paste the token (without
            the "Bearer " prefix — Swagger adds that automatically).

            ## Roles
            - CUSTOMER: browsing, cart, orders, reviews, chat
            - ADMIN: full product/order/inventory/user management

            Access tokens expire after 15 minutes — use POST /api/auth/refresh
            to obtain a new one without logging in again.
            """,

                contact = @Contact(
                        name = "Development Team",
                        email = "dev@ecomstore.com"
                )
        ),

        servers = {
                @Server(url = "http://localhost:9092", description = "Local development"),
                @Server(url = "https://api.ecomstore.com", description = "Production")
        },

        security = @SecurityRequirement(name = "bearerAuth")
        // @Operation(security = {})
)


// tells Swagger how authentication works (JWT bearer), so it can show the Authorize button and attach tokens automatically.
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Paste the access token obtained from /api/auth/login"
)
public class OpenApiConfig {

    // No fields, no methods needed.

}
