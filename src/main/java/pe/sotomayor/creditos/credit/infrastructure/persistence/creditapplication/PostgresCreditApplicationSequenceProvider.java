package pe.sotomayor.creditos.credit.infrastructure.persistence.creditapplication;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import pe.sotomayor.creditos.credit.domain.service.creditapplication
        .CreditApplicationSequenceProvider;

/**
 * Implementa el proveedor de secuencia de solicitudes de crédito
 * utilizando PostgreSQL.
 *
 * <p>
 * Obtiene el siguiente correlativo desde la secuencia
 * {@code credit_application_number_seq} definida en el esquema
 * de persistencia.
 * </p>
 *
 * @author DarwinG
 */
@Repository
public class PostgresCreditApplicationSequenceProvider
        implements CreditApplicationSequenceProvider {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * {@inheritDoc}
     */
    @Override
    public long nextValue() {

        Number value = (Number) entityManager
                .createNativeQuery(
                        "SELECT nextval('credit_application_number_seq')"
                )
                .getSingleResult();

        return value.longValue();
    }

}