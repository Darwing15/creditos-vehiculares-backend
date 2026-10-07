package pe.sotomayor.creditos.credit.infrastructure.persistence.credit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import pe.sotomayor.creditos.credit.domain.service.credit.CreditSequenceProvider;

/**
 * Implementación del proveedor de secuencias utilizando
 * una secuencia de PostgreSQL.
 *
 * <p>
 * Obtiene el siguiente valor mediante la función
 * {@code nextval()}, garantizando unicidad y seguridad
 * en escenarios concurrentes.
 * </p>
 *
 * @author DarwinG
 */
@Repository
public class PostgresCreditSequenceProvider
        implements CreditSequenceProvider {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public long nextValue() {

        Number value = (Number) entityManager
                .createNativeQuery(
                        "SELECT nextval('credit_number_seq')"
                )
                .getSingleResult();

        return value.longValue();

    }

}
