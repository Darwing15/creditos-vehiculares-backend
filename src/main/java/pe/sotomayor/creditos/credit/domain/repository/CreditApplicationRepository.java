package pe.sotomayor.creditos.credit.domain.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import pe.sotomayor.creditos.credit.domain.model.entity.CreditApplication;
import pe.sotomayor.creditos.credit.domain.model.enumeration.CreditApplicationStatus;

import pe.sotomayor.creditos.shared.persistence.BaseRepository;

/**
 * Define el contrato de persistencia para la entidad
 * {@link CreditApplication}.
 *
 * <p>
 * Abstrae las operaciones necesarias para la gestion de solicitudes
 * de credito, manteniendo desacoplada la capa de dominio de la
 * implementacion concreta de persistencia.
 * </p>
 *
 * <p>
 * Incluye operaciones especificas para consultar solicitudes por
 * sus identificadores publicos y por el estado de su ciclo de vida.
 * </p>
 *
 * @author DarwinG
 */
public interface CreditApplicationRepository
        extends BaseRepository<CreditApplication> {

    /**
     * Busca una solicitud de credito por su codigo publico.
     *
     * @param applicationCode codigo publico unico de la solicitud
     * @return la solicitud encontrada o vacio si no existe
     */
    Optional<CreditApplication> findByApplicationCode(
            UUID applicationCode
    );

    /**
     * Busca una solicitud de credito por su numero visible.
     *
     * @param applicationNumber numero visible de la solicitud
     * @return la solicitud encontrada o vacio si no existe
     */
    Optional<CreditApplication> findByApplicationNumber(
            String applicationNumber
    );

    /**
     * Verifica si existe una solicitud con el codigo publico indicado.
     *
     * @param applicationCode codigo publico de la solicitud
     * @return {@code true} si existe una solicitud con dicho codigo;
     * {@code false} en caso contrario
     */
    boolean existsByApplicationCode(
            UUID applicationCode
    );

    /**
     * Verifica si existe una solicitud con el numero indicado.
     *
     * @param applicationNumber numero visible de la solicitud
     * @return {@code true} si existe una solicitud con dicho numero;
     * {@code false} en caso contrario
     */
    boolean existsByApplicationNumber(
            String applicationNumber
    );

    /**
     * Obtiene las solicitudes de credito que se encuentran
     * en el estado indicado.
     *
     * <p>
     * Esta operacion sera utilizada principalmente para obtener
     * las solicitudes pendientes de evaluacion.
     * </p>
     *
     * @param status estado de las solicitudes a consultar
     * @return lista de solicitudes que poseen el estado indicado
     */
    List<CreditApplication> findAllByStatus(
            CreditApplicationStatus status
    );

    /**
     * Consulta sin incluir solicitudes de otras sucursales.
     *
     * @param status   estado solicitado
     * @param branchId sucursal del principal autenticado
     * @return solicitudes de la sucursal
     */
    List<CreditApplication> findAllByStatusAndBranchId(
            CreditApplicationStatus status,
            Long branchId
    );

    /**
     * Busca solicitudes no eliminadas de una sucursal, respetando los permisos de nombres.
     *
     * @param branchId         identificador de la sucursal autenticada
     * @param status           nombre del estado solicitado
     * @param query            fragmento literal de búsqueda
     * @param customersAllowed permite incluir nombres de clientes activos visibles
     * @param vehiclesAllowed  permite incluir datos de vehículos visibles
     * @param pageable         paginacion solicitada
     * @return pagina de solicitudes coincidentes, ordenada por identificador
     */
    @Query(value = """
            select a.* from credit_applications a
            left join customers c on :customersAllowed = true and c.id = a.customer_id
              and c.branch_id = a.branch_id and c.is_active = true and c.is_deleted = false
            left join vehicles v on :vehiclesAllowed = true and v.id = a.vehicle_id
              and v.branch_id = a.branch_id and v.is_deleted = false
            where a.branch_id = :branchId and a.is_deleted = false
              and a.status = cast(:status as credit_application_status)
              and position(lower(:query) in lower(concat(a.application_number, ' ', coalesce(c.full_name, ''), ' ',
                case when v.id is null then '' else concat(v.vehicle_type, ' ', v.brand, ' ', v.model) end,
                ' Cliente #', a.customer_id, ' Vehículo #', a.vehicle_id))) > 0
            order by a.id
            """, countQuery = """
            select count(*) from credit_applications a
            left join customers c on :customersAllowed = true and c.id = a.customer_id
              and c.branch_id = a.branch_id and c.is_active = true and c.is_deleted = false
            left join vehicles v on :vehiclesAllowed = true and v.id = a.vehicle_id
              and v.branch_id = a.branch_id and v.is_deleted = false
            where a.branch_id = :branchId and a.is_deleted = false
              and a.status = cast(:status as credit_application_status)
              and position(lower(:query) in lower(concat(a.application_number, ' ', coalesce(c.full_name, ''), ' ',
                case when v.id is null then '' else concat(v.vehicle_type, ' ', v.brand, ' ', v.model) end,
                ' Cliente #', a.customer_id, ' Vehículo #', a.vehicle_id))) > 0
            """, nativeQuery = true)
    Page<CreditApplication> searchPage(@Param("branchId") Long branchId,
                                       @Param("status") String status,
                                       @Param("query") String query,
                                       @Param("customersAllowed") boolean customersAllowed,
                                       @Param("vehiclesAllowed") boolean vehiclesAllowed,
                                       Pageable pageable);

    /**
     * Busca el identificador publico dentro de la sucursal autenticada.
     *
     * @param applicationCode identificador publico
     * @param branchId        sucursal del principal
     * @return solicitud o vacio
     */
    Optional<CreditApplication> findByApplicationCodeAndBranchId(
            UUID applicationCode,
            Long branchId
    );

    /**
     * Serializa las resoluciones de una solicitud para evitar transiciones concurrentes.
     *
     * @param applicationCode identificador publico
     * @param branchId        sucursal del principal
     * @return solicitud bloqueada hasta finalizar la transaccion
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from CreditApplication a where a.applicationCode=:code and a.branchId=:branchId")
    Optional<CreditApplication> findByApplicationCodeAndBranchIdForUpdate(
            @Param("code") UUID applicationCode,
            @Param("branchId") Long branchId
    );

}
