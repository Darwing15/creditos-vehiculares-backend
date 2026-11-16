package pe.sotomayor.creditos.vehicles.domain.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.sotomayor.creditos.shared.persistence.BaseRepository;

import pe.sotomayor.creditos.vehicles.domain.model.entity.Vehicle;
import pe.sotomayor.creditos.vehicles.domain.model.enumeration.VehicleStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data de unidades fisicas del inventario.
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
     * Recupera en lote unidades no eliminadas de una sucursal.
     *
     * @param branchId identificador de sucursal
     * @param ids      identificadores de unidades
     * @return unidades disponibles para consulta que coinciden con los identificadores
     */
    List<Vehicle> findAllByBranchIdAndIdIn(Long branchId, Collection<Long> ids);

    /**
     * @param branchId sucursal autenticada.
     * @param status   estado solicitado.
     * @return unidades filtradas por estado y sucursal.
     */
    List<Vehicle> findAllByBranchIdAndStatus(Long branchId, VehicleStatus status);

    /**
     * Consulta unidades no eliminadas de una sucursal con filtros combinados.
     *
     * @param branchId identificador de sucursal
     * @param query    fragmento literal de busqueda; vacio incluye todas
     * @param status   nombre del estado; vacio incluye todos
     * @param brand    marca exacta; vacia incluye todas
     * @param pageable paginacion solicitada
     * @return pagina ordenada por identificador
     */
    @Query(value = """
            select v.* from vehicles v where v.branch_id = :branchId and v.is_deleted = false
              and (:status = '' or v.status = cast(nullif(:status, '') as vehicle_status))
              and (:brand = '' or v.brand = :brand)
              and position(lower(:query) in lower(concat(v.brand, ' ', v.model, ' ',
                  cast(v.vehicle_code as text), ' ', coalesce(v.chassis_number, ''), ' ',
                  coalesce(v.engine_number, '')))) > 0
            order by v.id
            """, countQuery = """
            select count(*) from vehicles v where v.branch_id = :branchId and v.is_deleted = false
              and (:status = '' or v.status = cast(nullif(:status, '') as vehicle_status))
              and (:brand = '' or v.brand = :brand)
              and position(lower(:query) in lower(concat(v.brand, ' ', v.model, ' ',
                  cast(v.vehicle_code as text), ' ', coalesce(v.chassis_number, ''), ' ',
                  coalesce(v.engine_number, '')))) > 0
            """, nativeQuery = true)
    Page<Vehicle> searchPage(@Param("branchId") Long branchId,
                             @Param("query") String query,
                             @Param("status") String status,
                             @Param("brand") String brand,
                             Pageable pageable);

    /**
     * Cuenta unidades no eliminadas por estado en una sucursal.
     *
     * @param branchId identificador de sucursal
     * @return filas con estado y cantidad
     */
    @Query("select v.status, count(v) from Vehicle v where v.branchId = :branchId group by v.status")
    List<Object[]> countByStatus(@Param("branchId") Long branchId);

    /**
     * Obtiene las marcas distintas de unidades no eliminadas de una sucursal.
     *
     * @param branchId identificador de sucursal
     * @return marcas presentes en el inventario visible
     */
    @Query("select distinct v.brand from Vehicle v where v.branchId = :branchId")
    List<String> findBrands(@Param("branchId") Long branchId);

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
     * Unicidad global de chasis/motor incluso en unidades eliminadas;
     * 0 excluye ningun registro en alta.
     */
    @Query(value = "select exists(select 1 from vehicles where id<>:excludedId " +
            "and (upper(chassis_number)=upper(:chassis) " +
            "or upper(engine_number)=upper(:engine)))",
            nativeQuery = true)
    boolean existsIdentifiersIncludingDeleted(
            @Param("chassis") String chassis,
            @Param("engine") String engine,
            @Param("excludedId") Long excludedId
    );

}
