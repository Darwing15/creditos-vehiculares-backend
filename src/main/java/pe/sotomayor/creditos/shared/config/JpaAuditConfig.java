package pe.sotomayor.creditos.shared.config;

import org.springframework.context.annotation.Configuration;

import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuración de auditoría automática para entidades JPA.
 *
 * <p>
 * Habilita el mecanismo de Spring Data JPA Auditing,
 * permitiendo completar automáticamente los campos definidos
 * en {@link pe.sotomayor.creditos.shared.persistence.BaseEntity}
 * como fechas de creación, modificación y usuarios responsables.
 * </p>
 *
 * @author DarwinG
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {

}
