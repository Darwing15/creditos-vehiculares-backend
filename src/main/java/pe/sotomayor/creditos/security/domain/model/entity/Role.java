package pe.sotomayor.creditos.security.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import pe.sotomayor.creditos.shared.persistence.SoftDeleteEntity;

/**
 * Representa un rol de seguridad dentro del sistema.
 *
 * <p>
 * Un rol define la clasificación funcional asignada a un usuario y determina
 * sus capacidades dentro de la aplicación.
 * </p>
 *
 * <p>
 * La entidad utiliza eliminación lógica mediante {@link SoftDeleteEntity},
 * conservando el historial del registro y evitando su eliminación física.
 * </p>
 *
 * @author DarwinG
 */

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Role extends SoftDeleteEntity {

    @NotBlank
    @Size(max = 100)
    @Column(
            name = "name",
            nullable = false,
            unique = true,
            length = 100
    )
    private String name;

    @Size(max = 255)
    @Column(
            name = "description",
            columnDefinition = "TEXT"
    )
    private String description;

}
