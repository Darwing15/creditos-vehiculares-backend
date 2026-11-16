package pe.sotomayor.creditos.security.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
 * tecnologia de persistencia.
 * </p>
 *
 * @author DarwinG
 */
public interface UserRepository extends BaseRepository<User> {

    /**
     * Busca un usuario por su correo electronico.
     *
     * @param email correo electronico del usuario
     * @return el usuario encontrado o vacio si no existe.
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica si existe un usuario con el correo indicado.
     *
     * @param email correo electronico del usuario
     * @return {@code true} si existe un usuario con ese correo; de lo contrario {@code false}.
     */
    boolean existsByEmail(String email);

    /**
     * Verifica si existe otro usuario con el correo indicado.
     *
     * <p>
     * Utilizado durante la actualizacion para evitar conflictos de correo
     * electronico manteniendo la identidad del usuario actual.
     * </p>
     *
     * @param email correo electronico a validar
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
     * @param roleId identificador unico del rol
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
     * el proceso de autenticacion.
     *
     * <p>
     * Recupera el usuario utilizando su correo electronico e inicializa
     * de forma anticipada el rol y la sucursal mediante {@code JOIN FETCH},
     * evitando excepciones de tipo {@code LazyInitializationException}
     * cuando Spring Security construye el contexto de autenticacion.
     * </p>
     *
     * <p>
     * Este metodo debe utilizarse exclusivamente por la infraestructura
     * de seguridad ({@code CustomUserDetailsService}). Para el resto de
     * casos de uso del dominio debe emplearse {@link #findByEmail(String)}.
     * </p>
     *
     * @param email correo electronico del usuario
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
     * @return usuario encontrado, aunque esté inactivo, o vacio si no esta disponible
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
     * Busca usuarios no eliminados globalmente, con rol y sucursal cargados.
     *
     * @param query    fragmento literal de nombre, correo o rol; vacio incluye todos
     * @param roleId   identificador de rol o cero para omitir el filtro
     * @param branchId identificador de sucursal o cero para omitir el filtro
     * @param pageable paginacion y orden indicados por el servicio
     * @return pagina de usuarios, incluidos los inactivos
     */
    @EntityGraph(attributePaths = {"role", "branch"})
    @Query(value = """
            select u from User u join u.role r join u.branch b
            where u.deleted = false and (:roleId = 0 or r.id = :roleId)
              and (:branchId = 0 or b.id = :branchId)
              and (locate(lower(:query), lower(u.fullName)) > 0
                or locate(lower(:query), lower(u.email)) > 0
                or locate(lower(:query), lower(r.name)) > 0)
            """, countQuery = """
            select count(u) from User u join u.role r join u.branch b
            where u.deleted = false and (:roleId = 0 or r.id = :roleId)
              and (:branchId = 0 or b.id = :branchId)
              and (locate(lower(:query), lower(u.fullName)) > 0
                or locate(lower(:query), lower(u.email)) > 0
                or locate(lower(:query), lower(r.name)) > 0)
            """)
    Page<User> searchPage(@Param("query") String query,
                          @Param("roleId") Long roleId,
                          @Param("branchId") Long branchId,
                          Pageable pageable);

    /**
     * Obtiene los roles asociados a usuarios no eliminados, incluidos los inactivos.
     *
     * @return filas con identificador Long y nombre String de rol
     */
    @Query("select distinct r.id, r.name from User u join u.role r where u.deleted = false")
    List<Object[]> findVisibleRoleOptions();

    /**
     * Obtiene las sucursales asociadas a usuarios no eliminados, incluidos los inactivos.
     *
     * @return filas con identificador Long y nombre String de sucursal
     */
    @Query("select distinct b.id, b.name from User u join u.branch b where u.deleted = false")
    List<Object[]> findVisibleBranchOptions();

    /**
     * Busca una cuenta por correo normalizado y carga las relaciones de autenticacion.
     *
     * <p>No oculta estados inactivos/eliminados: el servicio evalua conjuntamente
     * los estados antes de habilitar el principal. Requiere unicidad normalizada en BD.</p>
     *
     * @param email correo de acceso normalizado, no nulo
     * @return cuenta con rol, sucursal y permisos, o vacio si no existe
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
