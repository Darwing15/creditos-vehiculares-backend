package pe.sotomayor.creditos.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Clase base para entidades que pueden activarse o desactivarse
 * sin ser eliminadas físicamente.
 *
 * <p>
 * Permite gestionar estados funcionales dentro del sistema,
 * diferenciando una entidad inactiva de una entidad eliminada
 * lógicamente.
 * </p>
 *
 * <p>
 * El atributo {@code active} representa la disponibilidad de la entidad:
 * </p>
 *
 * <ul>
 *     <li>{@code true}: entidad disponible para operaciones de negocio</li>
 *     <li>{@code false}: entidad temporalmente deshabilitada</li>
 * </ul>
 *
 * <p>
 * La eliminación lógica es gestionada por {@link SoftDeleteEntity}.
 * </p>
 *
 * @author DarwinG
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@MappedSuperclass
public abstract class ActivatableEntity extends SoftDeleteEntity {

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

}
