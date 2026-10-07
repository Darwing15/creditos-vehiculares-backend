package pe.sotomayor.creditos.security.application.mapper.role;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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
     * Construye un rol con sus metadatos sin asignarle permisos ni versión JPA.
     *
     * @param request nombre y descripción solicitados
     * @return entidad cuyos privilegios se administran por un caso de uso separado
     */
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    Role toEntity(CreateRoleRequest request);

    /**
     * Copia los metadatos editables y conserva los permisos y la versión actuales.
     *
     * @param request nombre y descripción que deben aplicarse
     * @param role    entidad existente que recibe los cambios
     */
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    void updateEntity(UpdateRoleRequest request, @MappingTarget Role role);

    /**
     * Convierte una entidad {@link Role} en un objeto de respuesta.
     *
     * @param role entidad del dominio
     * @return información del rol
     */
    RoleResponse toResponse(Role role);

}
