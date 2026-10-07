package pe.sotomayor.creditos.credit.domain.repository;

import jakarta.persistence.LockModeType;

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
     * Busca una solicitud de crédito por su código publico.
     *
     * @param applicationCode código público único de la solicitud
     * @return la solicitud encontrada o vacío si no existe
     */
    Optional<CreditApplication> findByApplicationCode(
            UUID applicationCode
    );

    /**
     * Busca una solicitud de crédito por su número visible.
     *
     * @param applicationNumber número visible de la solicitud
     * @return la solicitud encontrada o vacío si no existe
     */
    Optional<CreditApplication> findByApplicationNumber(
            String applicationNumber
    );

    /**
     * Verifica si existe una solicitud con el código público indicado.
     *
     * @param applicationCode código público de la solicitud
     * @return {@code true} si existe una solicitud con dicho código;
     * {@code false} en caso contrario
     */
    boolean existsByApplicationCode(
            UUID applicationCode
    );

    /**
     * Verifica si existe una solicitud con el número indicado.
     *
     * @param applicationNumber número visible de la solicitud
     * @return {@code true} si existe una solicitud con dicho número;
     * {@code false} en caso contrario
     */
    boolean existsByApplicationNumber(
            String applicationNumber
    );

    /**
     * Obtiene las solicitudes de crédito que se encuentran
     * en el estado indicado.
     *
     * <p>
     * Esta operación será utilizada principalmente para obtener
     * las solicitudes pendientes de evaluación.
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
     * Busca el identificador público dentro de la sucursal autenticada.
     *
     * @param applicationCode identificador público
     * @param branchId        sucursal del principal
     * @return solicitud o vacío
     */
    Optional<CreditApplication> findByApplicationCodeAndBranchId(
            UUID applicationCode,
            Long branchId
    );

    /**
     * Serializa las resoluciones de una solicitud para evitar transiciones concurrentes.
     *
     * @param applicationCode identificador público
     * @param branchId        sucursal del principal
     * @return solicitud bloqueada hasta finalizar la transacción
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from CreditApplication a where a.applicationCode=:code and a.branchId=:branchId")
    Optional<CreditApplication> findByApplicationCodeAndBranchIdForUpdate(
            @Param("code") UUID applicationCode,
            @Param("branchId") Long branchId
    );

}
