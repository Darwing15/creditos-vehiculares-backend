package pe.sotomayor.creditos.security.domain.repository;

import java.util.Optional;

import pe.sotomayor.creditos.security.domain.model.entity.Role;

import pe.sotomayor.creditos.shared.persistence.BaseRepository;

/**
 * Define el contrato de acceso al agregado {@link Role}.
 *
 * <p>
 * Abstrae las operaciones necesarias para gestionar la información del agregado
 * Role, manteniendo la capa de dominio desacoplada de los mecanismos concretos
 * de persistencia.
 * </p>
 *
 * @author DarwinG
 */
public interface RoleRepository extends BaseRepository<Role> {

    /**
     * Busca un rol por su nombre.
     *
     * @param name nombre único del rol
     * @return el rol encontrado o vacío si no existe
     */
    Optional<Role> findByName(String name);

    /**
     * Verifica si ya existe un rol registrado con el nombre indicado.
     *
     * @param name nombre del rol
     * @return {@code true} si existe un rol con ese nombre; en caso contrario,
     * {@code false}
     */
    boolean existsByName(String name);

    /**
     * Verifica si existe otro rol con el mismo nombre.
     *
     * @param name nombre del rol
     * @param id   identificador del rol a excluir
     * @return {@code true} si existe otro rol con ese nombre; en caso contrario,
     * {@code false}
     */
    boolean existsByNameAndIdNot(String name, Long id);

}
