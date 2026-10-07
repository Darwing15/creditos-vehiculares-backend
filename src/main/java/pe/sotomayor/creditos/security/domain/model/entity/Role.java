package pe.sotomayor.creditos.security.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import pe.sotomayor.creditos.shared.persistence.SoftDeleteEntity;

import java.util.HashSet;
import java.util.Set;

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

    /**
     * Autoridades concedidas al rol, inicialmente vacías para evitar privilegios
     * implícitos al crearlo. La carga diferida se resuelve dentro de una transacción.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission", nullable = false, length = 100)
    @Builder.Default
    private Set<String> permissions = new HashSet<>();

    /**
     * Versión de concurrencia administrada por Hibernate; una escritura basada en
     * una versión anterior se rechaza para evitar la pérdida de cambios.
     */
    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

}
