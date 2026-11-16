package pe.sotomayor.creditos.vehicles.application.usecase;

import java.util.List;

import pe.sotomayor.creditos.vehicles.application.dto.response.VehicleOptionResponse;
import pe.sotomayor.creditos.vehicles.application.dto.response.VehicleOverviewResponse;
import pe.sotomayor.creditos.vehicles.application.dto.response.VehiclePageResponse;

import pe.sotomayor.creditos.shared.exception.ValidationException;

import pe.sotomayor.creditos.vehicles.domain.model.enumeration.VehicleStatus;

/**
 * Caso de uso de consulta de inventario visible para la sucursal actual.
 *
 * @author DarwinG
 */
public interface ListVehiclesUseCase {
    /**
     * @param status filtro opcional; null incluye todos los estados.
     * @return unidades no eliminadas de la sucursal actual.
     */
    List<VehicleOptionResponse> list(VehicleStatus status);

    /**
     * Busca unidades no eliminadas de la sucursal autenticada.
     *
     * @param query  fragmento literal opcional de marca, modelo, codigo, chasis o motor
     * @param status estado opcional de la unidad
     * @param brand  marca opcional; vacia incluye todas
     * @param page   pagina desde cero, hasta 1 000 000
     * @param size   tamaño de pagina entre 1 y 100
     * @return pagina ordenada por identificador y sus totales
     * @throws ValidationException si la busqueda o paginacion son invalidas
     */
    VehiclePageResponse searchPage(String query, VehicleStatus status, String brand, int page, int size);

    /**
     * Resume todas las unidades visibles de la sucursal, sin aplicar los filtros del listado.
     *
     * @return totales por estado y marcas disponibles en la sucursal
     */
    VehicleOverviewResponse overview();
}
