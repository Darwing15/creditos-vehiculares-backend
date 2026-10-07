package pe.sotomayor.creditos.security.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import pe.sotomayor.creditos.shared.persistence.ActivatableEntity;

/**
 * Representa una sucursal de la empresa.
 *
 * <p>
 * Una sucursal identifica una oficina o sede desde la cual operan los usuarios
 * del sistema.
 * </p>
 *
 * <p>
 * La entidad permite activación y desactivación lógica mediante
 * {@link ActivatableEntity}, conservando la información histórica del registro.
 * </p>
 *
 * @author DarwinG
 */
@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Branch extends ActivatableEntity {

    /**
     * Código único de la sucursal.
     */
    @NotBlank
    @Size(max = 20)
    @Column(
            name = "code",
            nullable = false,
            unique = true,
            length = 20
    )
    private String code;

    /**
     * Nombre de la sucursal.
     */
    @NotBlank
    @Size(max = 100)
    @Column(
            name = "name",
            nullable = false,
            length = 100
    )
    private String name;

    /**
     * Dirección física de la sucursal.
     */
    @NotBlank
    @Size(max = 255)
    @Column(
            name = "address",
            nullable = false,
            length = 255
    )
    private String address;

    /**
     * Número telefónico de contacto.
     */
    @Size(max = 20)
    @Column(
            name = "phone",
            length = 20
    )
    private String phone;

    /**
     * Correo electrónico de contacto.
     */
    @Email
    @Size(max = 120)
    @Column(
            name = "email",
            length = 120
    )
    private String email;

    /**
     * Identificador del usuario responsable de la sucursal.
     * <p>
     * La relación con la entidad User será incorporada cuando
     * la clave foránea sea creada mediante una migración Flyway.
     */
    @Column(name = "manager_id")
    private Long managerId;

    /**
     * Versión de concurrencia administrada por Hibernate; una escritura basada en
     * una versión anterior se rechaza para evitar la pérdida de cambios.
     */
    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

}