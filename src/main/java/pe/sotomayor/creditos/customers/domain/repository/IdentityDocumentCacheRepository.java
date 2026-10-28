package pe.sotomayor.creditos.customers.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.shared.persistence.BaseRepository;
import pe.sotomayor.creditos.customers.domain.model.entity.IdentityDocumentCache;

/**
 * Persistencia del ultimo resultado; la unicidad y el reemplazo son arbitrados por PostgreSQL.
 *
 * @author DarwinG
 */
public interface IdentityDocumentCacheRepository extends BaseRepository<IdentityDocumentCache> {
    /**
     * @return ultimo resultado disponible, cuya vigencia comprueba el dominio.
     */
    @Transactional(readOnly = true)
    Optional<IdentityDocumentCache> findByDocumentNumber(String documentNumber);

    /**
     * Guarda en una transaccion corta sin mantener bloqueos durante la consulta externa.
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO identity_document_cache
                (document_number, given_names, first_surname, second_surname, full_name, provider,
                 consulted_at, expires_at, created_by, updated_by, created_at, updated_at)
            VALUES (:#{#entry.documentNumber}, :#{#entry.givenNames}, :#{#entry.firstSurname},
                    :#{#entry.secondSurname}, :#{#entry.fullName}, :#{#entry.provider},
                    :#{#entry.consultedAt}, :#{#entry.expiresAt}, :#{#entry.createdBy},
                    :#{#entry.updatedBy}, :#{#entry.consultedAt}, :#{#entry.consultedAt})
            ON CONFLICT (document_number) DO UPDATE SET
                given_names = EXCLUDED.given_names, first_surname = EXCLUDED.first_surname,
                second_surname = EXCLUDED.second_surname, full_name = EXCLUDED.full_name,
                provider = EXCLUDED.provider, consulted_at = EXCLUDED.consulted_at,
                expires_at = EXCLUDED.expires_at, updated_by = EXCLUDED.updated_by,
                updated_at = EXCLUDED.updated_at
            WHERE identity_document_cache.consulted_at <= EXCLUDED.consulted_at
            """, nativeQuery = true)
    void upsert(@Param("entry") IdentityDocumentCache entry);
}
