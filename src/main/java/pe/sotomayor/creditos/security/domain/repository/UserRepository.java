package pe.sotomayor.creditos.security.domain.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.shared.persistence.BaseRepository;

import java.util.List;
import java.util.Optional;

/**
 * Define el contrato de persistencia para la entidad {@link User}.
 *
 * <p>
 * Abstrae las operaciones de acceso a datos del agregado User,
 * permitiendo que la capa de dominio permanezca desacoplada de la
 * tecnología de persistencia.
 * </p>
 *
 * @author DarwinG
 */
public interface UserRepository extends BaseRepository<User> {

    /**
     * Busca un usuario por su correo electrónico.
     *
     * @param email correo electrónico del usuario
     * @return el usuario encontrado o vacío si no existe.
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica si existe un usuario con el correo indicado.
     *
     * @param email correo electrónico del usuario
     * @return {@code true} si existe un usuario con ese correo; de lo contrario {@code false}.
     */
    boolean existsByEmail(String email);

    /**
     * Verifica si existe otro usuario con el correo indicado.
     *
     * <p>
     * Utilizado durante la actualización para evitar conflictos de correo
     * electrónico manteniendo la identidad del usuario actual.
     * </p>
     *
     * @param email correo electrónico a validar
     * @param id    identificador del usuario excluido
     * @return {@code true} si existe otro usuario con ese correo
     */
    boolean existsByEmailAndIdNot(
            String email,
            Long id);

    /**
     * Verifica si existen usuarios asignados al rol indicado.
     *
     * <p>
     * Permite validar reglas de negocio antes de realizar operaciones
     * que afecten la disponibilidad del rol.
     * </p>
     *
     * @param roleId identificador único del rol
     * @return {@code true} si existen usuarios asignados al rol;
     * de lo contrario, {@code false}
     */
    boolean existsByRoleId(Long roleId);

    /**
     * Verifica si existen usuarios asociados a una sucursal.
     *
     * @param branchId identificador de la sucursal
     * @return {@code true} si existe al menos un usuario asociado;
     * de lo contrario, {@code false}
     */
    boolean existsByBranchId(Long branchId);

    /**
     * Obtiene un usuario junto con las asociaciones requeridas durante
     * el proceso de autenticación.
     *
     * <p>
     * Recupera el usuario utilizando su correo electrónico e inicializa
     * de forma anticipada el rol y la sucursal mediante {@code JOIN FETCH},
     * evitando excepciones de tipo {@code LazyInitializationException}
     * cuando Spring Security construye el contexto de autenticación.
     * </p>
     *
     * <p>
     * Este método debe utilizarse exclusivamente por la infraestructura
     * de seguridad ({@code CustomUserDetailsService}). Para el resto de
     * casos de uso del dominio debe emplearse {@link #findByEmail(String)}.
     * </p>
     *
     * @param email correo electrónico del usuario
     * @return usuario con el rol y la sucursal cargados
     */
    @EntityGraph(attributePaths = {
            "role",
            "branch"
    })
    Optional<User> findUserWithRoleAndBranchByEmail(String email);

    /**
     * Busca un usuario no eliminado y carga las relaciones necesarias para su DTO.
     *
     * @param id identificador del usuario
     * @return usuario encontrado, aunque esté inactivo, o vacío si no está disponible
     */
    @EntityGraph(attributePaths = {
            "role",
            "branch"
    })
    Optional<User> findByIdAndDeletedFalse(Long id);

    /**
     * Recupera usuarios administrables con rol y sucursal, sin cargar permisos.
     *
     * @return usuarios no eliminados, incluidos los inactivos, sin orden garantizado
     */
    @EntityGraph(attributePaths = {
            "role",
            "branch"
    })
    List<User> findAllByDeletedFalse();

    /**
     * Busca una cuenta por correo normalizado y carga las relaciones de autenticación.
     *
     * <p>No oculta estados inactivos/eliminados: el servicio evalúa conjuntamente
     * los estados antes de habilitar el principal. Requiere unicidad normalizada en BD.</p>
     *
     * @param email correo de acceso normalizado, no nulo
     * @return cuenta con rol, sucursal y permisos, o vacío si no existe
     */
    @EntityGraph(
            attributePaths = {
                    "role",
                    "branch",
                    "role.permissions"
            }
    )
    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findForAuthenticationByEmail(
            @Param("email") String email);

    /**
     * Carga por identidad estable el estado utilizado para verificar un JWT.
     *
     * @param id ID positivo extraído de un token cuya firma ya fue validada
     * @return cuenta y relaciones necesarias, o vacío si no existe
     */
    @EntityGraph(
            attributePaths = {
                    "role",
                    "branch",
                    "role.permissions"
            }
    )
    @Query("select u from User u where u.id = :id")
    Optional<User> findForAuthenticationById(
            @Param("id") Long id);

}
