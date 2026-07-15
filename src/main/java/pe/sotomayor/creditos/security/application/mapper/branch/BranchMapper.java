package pe.sotomayor.creditos.security.application.mapper.branch;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import pe.sotomayor.creditos.security.application.dto.branch.request.CreateBranchRequest;
import pe.sotomayor.creditos.security.application.dto.branch.request.UpdateBranchRequest;
import pe.sotomayor.creditos.security.application.dto.branch.response.BranchResponse;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;

/**
 * Realiza las conversiones entre entidades Branch y DTOs
 * del módulo de seguridad.
 *
 * <p>
 * Centraliza la transformación de información entre la capa Application
 * y el modelo de dominio, evitando conversiones manuales dentro
 * de los casos de uso.
 * </p>
 *
 * @author DarwinG
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface BranchMapper {

    /**
     * Convierte una solicitud de creación en una entidad {@link Branch}.
     *
     * <p>
     * Los campos de identidad, auditoría y estado operativo son gestionados
     * por el caso de uso correspondiente.
     * </p>
     *
     * @param request información para registrar una sucursal
     * @return entidad Branch
     */
    @Mapping(
            target = "id",
            ignore = true
    )
    @Mapping(
            target = "active",
            ignore = true
    )
    Branch toEntity(CreateBranchRequest request);

    /**
     * Actualiza los datos administrativos modificables
     * de una sucursal existente.
     *
     * @param request información actualizada de la sucursal
     * @param branch  entidad existente a modificar
     */
    void updateEntity(UpdateBranchRequest request,
                      @MappingTarget Branch branch);

    /**
     * Convierte una entidad {@link Branch} en un objeto de respuesta.
     *
     * @param branch entidad del dominio
     * @return información de la sucursal
     */
    BranchResponse toResponse(Branch branch);

}
