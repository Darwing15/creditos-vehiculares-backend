package pe.sotomayor.creditos.security.application.mapper.user;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import pe.sotomayor.creditos.security.application.dto.user.request.CreateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.request.UpdateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;

import pe.sotomayor.creditos.security.domain.model.entity.User;

/**
 * Realiza las conversiones entre entidades User y DTOs de respuesta
 * del módulo de seguridad.
 *
 * <p>
 * Centraliza la transformación de información entre la capa Application
 * y el modelo de dominio.
 * </p>
 *
 * @author DarwinG
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    /**
     * Convierte una solicitud de creación en una entidad {@link User}.
     *
     * <p>
     * Las relaciones con Role y Branch son asignadas posteriormente
     * por el caso de uso debido a que requieren validaciones de negocio.
     * </p>
     *
     * @param request información para registrar un usuario
     * @return entidad User
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "branch", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "active", ignore = true)
    User toEntity(CreateUserRequest request);

    /**
     * Actualiza los datos administrativos modificables de un usuario existente.
     *
     * <p>
     * Las relaciones con Role y Branch son gestionadas por el caso de uso,
     * ya que requieren validaciones de existencia y reglas de negocio.
     * </p>
     *
     * @param request información actualizada del usuario
     * @param user    entidad existente a modificar
     */
    @Mapping(
            target = "id",
            ignore = true
    )
    @Mapping(
            target = "role",
            ignore = true
    )
    @Mapping(
            target = "branch",
            ignore = true
    )
    @Mapping(
            target = "password",
            ignore = true
    )
    @Mapping(
            target = "active",
            ignore = true
    )
    void updateEntity(UpdateUserRequest request, @MappingTarget User user);

    /**
     * Convierte una entidad {@link User} en un objeto de respuesta.
     *
     * @param user entidad del dominio
     * @return información del usuario
     */
    @Mapping(
            source = "role.id",
            target = "roleId"
    )
    @Mapping(
            source = "role.name",
            target = "roleName"
    )
    @Mapping(
            source = "branch.id",
            target = "branchId"
    )
    @Mapping(
            source = "branch.name",
            target = "branchName"
    )
    UserResponse toResponse(User user);

}
