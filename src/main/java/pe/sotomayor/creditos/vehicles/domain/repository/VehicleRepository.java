package pe.sotomayor.creditos.vehicles.domain.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import pe.sotomayor.creditos.shared.persistence.BaseRepository;

import pe.sotomayor.creditos.vehicles.domain.model.entity.Vehicle;

import pe.sotomayor.creditos.vehicles.domain.model.enumeration.VehicleStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data de unidades físicas del inventario.
 *
 * @author DarwinG
 */
public interface VehicleRepository extends BaseRepository<Vehicle> {
    /**
     * @param branchId sucursal autenticada.
     * @return unidades de todos los estados en sucursal.
     */
    List<Vehicle> findAllByBranchId(Long branchId);

    /**
     * @param branchId sucursal autenticada.
     * @param status   estado solicitado.
     * @return unidades filtradas por estado y sucursal.
     */
    List<Vehicle> findAllByBranchIdAndStatus(Long branchId, VehicleStatus status);

    /**
     * Bloquea una unidad para evitar dos reservas concurrentes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehicle v where v.id=:vehicleId and v.branchId=:branchId")
    Optional<Vehicle> findByIdAndBranchIdForUpdate(
            @Param("vehicleId") Long vehicleId,
            @Param("branchId") Long branchId
    );

    /**
     * Consulta publica de la sucursal actual.
     */
    Optional<Vehicle> findByVehicleCodeAndBranchId(UUID code, Long branchId);

    /**
     * Serializa la actualizacion y comparte bloqueo con los flujos de inventario.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Vehicle e where e.vehicleCode=:code and e.branchId=:branchId")
    Optional<Vehicle> findByVehicleCodeAndBranchIdForUpdate(
            @Param("code") UUID code,
            @Param("branchId") Long branchId
    );

    /**
     * Unicidad global de chasis/motor incluso en unidades eliminadas; 0 excluye ningun registro en alta.
     */
    @Query(value = "select exists(select 1 from vehicles where id<>:excludedId and (chassis_number=:chassis or engine_number=:engine))",
            nativeQuery = true)
    boolean existsIdentifiersIncludingDeleted(
            @Param("chassis") String chassis,
            @Param("engine") String engine,
            @Param("excludedId") Long excludedId
    );

}
