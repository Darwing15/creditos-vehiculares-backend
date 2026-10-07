package pe.sotomayor.creditos.customers.domain.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.sotomayor.creditos.customers.domain.model.entity.Customer;

import pe.sotomayor.creditos.shared.persistence.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrato de consulta del agregado Customer.
 *
 * @author DarwinG
 */
public interface CustomerRepository extends BaseRepository<Customer> {
    /**
     * @param branchId sucursal actual.
     * @return clientes activos no eliminados de esa sucursal.
     */
    List<Customer> findAllByBranchIdAndActiveTrue(Long branchId);

    /**
     * @return cliente activo dentro de la sucursal, si existe
     */
    Optional<Customer> findByIdAndBranchIdAndActiveTrue(
            Long id,
            Long branchId
    );

    /**
     * Consulta publica de la sucursal actual.
     */
    Optional<Customer> findByCustomerCodeAndBranchId(
            UUID code,
            Long branchId
    );

    /**
     * Serializa la actualizacion y comparte bloqueo con los flujos de inventario.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Customer e where e.customerCode=:code and e.branchId=:branchId")
    Optional<Customer> findByCustomerCodeAndBranchIdForUpdate(
            @Param("code") UUID code,
            @Param("branchId") Long branchId
    );

    /**
     * La unicidad del documento es global e incluye registros eliminados de V3.
     */
    @Query(value = "select exists(select 1 from customers where document_type=:type and document_number=:number)",
            nativeQuery = true)
    boolean existsDocumentIncludingDeleted(
            @Param("type") String type,
            @Param("number") String number
    );
}
