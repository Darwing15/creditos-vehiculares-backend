package pe.sotomayor.creditos.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * Clase base para entidades que implementan eliminación lógica.
 *
 * <p>
 * Permite conservar físicamente los registros en la base de datos,
 * marcándolos como eliminados mediante indicadores de estado.
 * </p>
 *
 * <p>
 * Las entidades que heredan de esta clase serán filtradas automáticamente
 * por Hibernate mediante la restricción definida con {@link SQLRestriction},
 * excluyendo los registros marcados como eliminados.
 * </p>
 *
 * @author DarwinG
 */

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@MappedSuperclass
@SQLRestriction("is_deleted = false")
public abstract class SoftDeleteEntity extends BaseEntity {

    /**
     * Fecha y hora en la que el registro fue marcado como eliminado.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Indica si el registro fue eliminado lógicamente.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

}
