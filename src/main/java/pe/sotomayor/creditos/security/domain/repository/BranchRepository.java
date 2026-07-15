package pe.sotomayor.creditos.security.domain.repository;

import java.util.Optional;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.shared.persistence.BaseRepository;

/**
 * Define el contrato de persistencia para la entidad {@link Branch}.
 *
 * <p>
 * Abstrae las operaciones necesarias para la gestión de sucursales,
 * manteniendo desacoplada la capa de dominio de la implementación
 * concreta de persistencia.
 * </p>
 *
 * <p>
 * Incluye operaciones específicas para validar la unicidad del código
 * de sucursal, regla necesaria para los casos de uso de creación y
 * actualización.
 * </p>
 *
 * @author DarwinG
 */
public interface BranchRepository extends BaseRepository<Branch> {

    /**
     * Busca una sucursal por su código.
     *
     * @param code código único de la sucursal
     * @return la sucursal encontrada o vacío si no existe.
     */
    Optional<Branch> findByCode(String code);

    /**
     * Verifica si existe una sucursal con el código indicado.
     *
     * @param code código de la sucursal
     * @return {@code true} si existe una sucursal con ese código;
     * {@code false} en caso contrario
     */
    boolean existsByCode(String code);

}
