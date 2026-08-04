package pe.sotomayor.creditos.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI utilizada para documentar
 * los servicios REST del sistema.
 *
 * <p>
 * Define la información general de la API y el esquema
 * de autenticación JWT utilizado por los endpoints
 * protegidos.
 * </p>
 *
 * @author DarwinG
 */
@Configuration
public class OpenApiConfig {

    /**
     * Nombre del esquema de seguridad utilizado por OpenAPI.
     */
    private static final String SECURITY_SCHEME = "Bearer Authentication";

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Configura la especificación OpenAPI.
     *
     * @return configuración principal de OpenAPI
     */
    @Bean
    public OpenAPI openAPI() {

        return new OpenAPI()

                .info(apiInfo())

                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(SECURITY_SCHEME))

                .schemaRequirement(
                        SECURITY_SCHEME,
                        securityScheme());

    }

    /**
     * Información general de la API.
     */
    private Info apiInfo() {

        return new Info()

                .title(applicationName)

                .description("""
                        API REST para la administración del sistema
                        de créditos.
                        """)

                .version("1.0.0")

                .contact(
                        new Contact()
                                .name("Raul Sotomayor")
                                .email("raul@sotomayor.pe"))

                .license(
                        new License()
                                .name("MIT"));

    }

    /**
     * Configura el esquema de autenticación Bearer JWT.
     */
    private SecurityScheme securityScheme() {

        return new SecurityScheme()

                .name("Authorization")

                .type(SecurityScheme.Type.HTTP)

                .scheme("bearer")

                .bearerFormat("JWT");

    }

}
