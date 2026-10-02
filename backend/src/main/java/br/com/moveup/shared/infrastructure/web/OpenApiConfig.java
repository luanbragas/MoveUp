package br.com.moveup.shared.infrastructure.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados do contrato (BACKEND-PATTERN, seção 9): segurança {@code bearerAuth} (ID token do
 * Firebase) em todas as rotas e servidor relativo, para o arquivo gerado não depender da porta.
 */
@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
    info =
        @Info(
            title = "MoveUp API",
            version = "v1",
            description =
                "API do app MoveUp. Erros em application/problem+json (RFC 9457) com `code`"
                    + " estável."),
    servers = @Server(url = "/"),
    security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "ID token do Firebase Auth")
class OpenApiConfig {}
