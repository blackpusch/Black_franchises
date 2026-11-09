package com.frank.franchises.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Franchises Management Reactive REST API",
                version = "1.0.0",
                description = "API REST Reactiva no bloqueante construida con Spring Boot 3 WebFlux y MongoDB Reactivo para la gestión integral de franquicias, sucursales y productos con control de stock.",
                contact = @Contact(
                        name = "Frank",
                        url = "https://github.com/blackpusch/Black_franchises"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0.html"
                )
        ),
        servers = {
                @Server(url = "/", description = "Servidor Actual (Local / Cloud)")
        }
)
public class OpenApiConfig {
}
