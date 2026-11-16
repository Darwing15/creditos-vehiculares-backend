package pe.sotomayor.creditos.credit.application.usecase.creditapplication;

import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationPageResponse;
import pe.sotomayor.creditos.credit.application.dto.creditapplication.response.CreditApplicationResponse;

import pe.sotomayor.creditos.credit.domain.model.enumeration.CreditApplicationStatus;

import pe.sotomayor.creditos.shared.exception.ValidationException;

import java.util.List;

/**
 * Define el caso de uso para listar solicitudes de credito
 * segun su estado.
 *
 * @author DarwinG
 */
public interface ListCreditApplicationsUseCase {

    /**
     * Obtiene las solicitudes de credito que se encuentran
     * en el estado indicado.
     *
     * @param status estado de las solicitudes a consultar.
     * @return solicitudes encontradas.
     */
    List<CreditApplicationResponse> listByStatus(
            CreditApplicationStatus status
    );

    /**
     * Busca solicitudes de la sucursal autenticada por estado y texto permitido.
     * La busqueda por nombres de cliente y vehiculo depende de sus permisos de lectura.
     *
     * @param status estado obligatorio de las solicitudes
     * @param query  fragmento literal opcional
     * @param page   pagina desde cero, hasta 1 000 000
     * @param size   tamaño de pagina entre 1 y 100
     * @return pagina ordenada por identificador y sus totales
     * @throws ValidationException si el estado o los parametros son invalidos
     */
    CreditApplicationPageResponse searchPage(
            CreditApplicationStatus status,
            String query,
            int page,
            int size
    );

}