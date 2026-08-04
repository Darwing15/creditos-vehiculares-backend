package pe.sotomayor.creditos.security.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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
 * Representa un usuario del sistema.
 *
 * <p>
 * Un usuario es una persona autorizada para acceder a la aplicación.
 * Cada usuario pertenece a un rol de seguridad y a una sucursal.
 * </p>
 *
 * <p>
 * La autenticación se realiza mediante correo electrónico y contraseña.
 * </p>
 *
 * @author DarwinG
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User extends ActivatableEntity {

    /**
     * Nombre completo del usuario.
     */
    @NotBlank
    @Size(max = 150)
    @Column(
            name = "full_name",
            nullable = false,
            length = 150
    )
    private String fullName;

    /**
     * Correo electrónico del usuario.
     * <p>
     * Debe ser único dentro del sistema y será utilizado
     * como identificador para el inicio de sesión.
     */
    @NotBlank
    @Email
    @Size(max = 120)
    @Column(
            name = "email",
            nullable = false,
            unique = true,
            length = 120
    )
    private String email;

    /**
     * Contraseña cifrada del usuario.
     * <p>
     * El valor almacenado corresponde al resultado del
     * algoritmo de cifrado utilizado por la aplicación.
     */
    @NotBlank
    @Size(max = 255)
    @Column(
            name = "password",
            nullable = false,
            length = 255
    )
    private String password;

    /**
     * Versión del token de autenticación.
     *
     * <p>
     * Este valor se incrementa cada vez que el usuario modifica su
     * contraseña o cuando se requiere invalidar todas las sesiones
     * activas.
     * </p>
     *
     * <p>
     * Cada token JWT almacena la versión vigente al momento de ser
     * generado. Durante la autenticación dicha versión es comparada
     * con la registrada en la base de datos para determinar si el
     * token continúa siendo válido.
     * </p>
     */
    @Column(
            name = "token_version",
            nullable = false
    )
    private Long tokenVersion;

    /**
     * Rol asignado al usuario.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "role_id",
            nullable = false
    )
    private Role role;

    /**
     * Sucursal a la que pertenece el usuario.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "branch_id",
            nullable = false
    )
    private Branch branch;

    /**
     * Incrementa la versión del token utilizada para invalidar
     * los JWT emitidos previamente.
     */
    public void increaseTokenVersion() {

        this.tokenVersion++;
    }

}
