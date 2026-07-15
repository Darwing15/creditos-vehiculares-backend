package pe.sotomayor.creditos.security.application.mapper.role;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import pe.sotomayor.creditos.security.application.dto.role.request.CreateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.request.UpdateRoleRequest;
import pe.sotomayor.creditos.security.application.dto.role.response.RoleResponse;
import pe.sotomayor.creditos.security.domain.model.entity.Role;

/**
 * Realiza conversiones estructurales entre objetos de aplicación y dominio.
 *
 * <p>
 * Este componente no contiene reglas de negocio; su responsabilidad se limita
 * a transformar información entre capas.
 * </p>
 *
 * @author DarwinG
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface RoleMapper {

    /**
     * Convierte una solicitud de creación en una entidad {@link Role}.
     *
     * @param request información para registrar un nuevo rol
     * @return entidad Role
     */
    Role toEntity(CreateRoleRequest request);

    /**
     * Actualiza los datos editables de una entidad {@link Role}.
     *
     * <p>
     * La identidad de la entidad no se modifica porque pertenece al agregado
     * existente.
     * </p>
     *
     * @param request información actualizada del rol
     * @param role    entidad existente a modificar
     */
    void updateEntity(UpdateRoleRequest request, @MappingTarget Role role);

    /**
     * Convierte una entidad {@link Role} en un objeto de respuesta.
     *
     * @param role entidad del dominio
     * @return información del rol
     */
    RoleResponse toResponse(Role role);

}
