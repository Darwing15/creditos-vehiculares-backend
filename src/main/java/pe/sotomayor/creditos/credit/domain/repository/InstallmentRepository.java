package pe.sotomayor.creditos.credit.domain.repository;

import pe.sotomayor.creditos.credit.domain.model.entity.Installment;
import pe.sotomayor.creditos.shared.persistence.BaseRepository;

import java.util.List;

/**
 * Repositorio de dominio para la persistencia y consulta
 * de las cuotas pertenecientes a un crédito.
 *
 * @author DarwinG
 */
public interface InstallmentRepository
        extends BaseRepository<Installment> {

    /**
     * Obtiene el cronograma de cuotas de un crédito
     * ordenado por número de cuota.
     *
     * @param creditId identificador interno del crédito
     * @return cuotas pertenecientes al crédito en orden contractual
     */
    List<Installment> findAllByCreditIdOrderByInstallmentNumberAsc(
            Long creditId
    );

}
