package br.senac.tads.dsw.dadospessoais.seguranca;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// Declara o esquema de autenticação JWT no documento OpenAPI gerado pelo SpringDoc.
// Esta anotação é puramente descritiva — não altera nenhuma regra do Spring Security.
// O name "bearerJwt" é o identificador referenciado em @SecurityRequirement nos controllers.
@Configuration
@SecurityScheme(
 name = "bearerJwt",
 type = SecuritySchemeType.HTTP,
 scheme = "bearer",
 bearerFormat = "JWT",
 description = "Cole o token JWT obtido em POST /login"
)
public class SwaggerConfig {

}