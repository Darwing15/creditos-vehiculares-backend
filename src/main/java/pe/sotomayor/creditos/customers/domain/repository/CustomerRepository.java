package pe.sotomayor.creditos.customers.domain.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.sotomayor.creditos.customers.domain.model.entity.Customer;

import pe.sotomayor.creditos.shared.persistence.BaseRepository;

import java.util.Collection;
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
     * Recupera en lote clientes activos y no eliminados de una sucursal.
     *
     * @param branchId identificador de sucursal
     * @param ids      identificadores de clientes
     * @return clientes disponibles que coinciden con los identificadores
     */
    List<Customer> findAllByBranchIdAndIdInAndActiveTrue(
            Long branchId, Collection<Long> ids);

    /**
     * Busca clientes activos y no eliminados en la sucursal indicada.
     *
     * @param branchId identificador de sucursal
     * @param search   fragmento literal, sin distinguir mayusculas, de los campos mostrados
     * @param pageable paginacion y orden solicitados por el servicio
     * @return pagina de clientes coincidentes
     */
    @Query(value = """
            select c from Customer c
            where c.branchId = :branchId 
                        and c.active = true
              and locate(lower(:search), 
                          lower(concat(c.fullName, ' ', 
                                      c.documentNumber, ' ',
                  coalesce(c.phone, ''), ' ', 
                              coalesce(c.address, '')))) > 0
            """, countQuery = """
            select count(c) from Customer c
            where c.branchId = :branchId 
            and c.active = true
              and locate(lower(:search), 
              lower(concat(c.fullName, ' ', c.documentNumber, ' ',
                  coalesce(c.phone, ''), ' ', coalesce(c.address, '')))) > 0
            """)
    Page<Customer> searchActiveInBranch(@Param("branchId") Long branchId,
                                        @Param("search") String search,
                                        Pageable pageable);

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
    @Query(value = "select exists(select 1 from customers where document_type=:type and "
            + "((:type='CE' and upper(document_number)=upper(:number)) "
            + "or (:type<>'CE' and document_number=:number)))",
            nativeQuery = true)
    boolean existsDocumentIncludingDeleted(
            @Param("type") String type,
            @Param("number") String number
    );
}
