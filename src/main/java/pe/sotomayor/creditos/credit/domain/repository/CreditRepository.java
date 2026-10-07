package pe.sotomayor.creditos.credit.domain.repository;

import pe.sotomayor.creditos.credit.domain.model.entity.Credit;
import pe.sotomayor.creditos.shared.persistence.BaseRepository;

/**
 * Define el contrato de persistencia para la entidad {@link Credit}.
 *
 * <p>
 * Mantiene desacoplado el dominio de la implementación concreta
 * de persistencia.
 * </p>
 *
 * @author DarwinG
 */
public interface CreditRepository extends BaseRepository<Credit> {
}