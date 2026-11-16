package pe.sotomayor.creditos.vehicles.application.dto.response;

import java.util.List;

/**
 * Totales y marcas de toda la sucursal, sin filtros de la tabla.
 * @author DarwinG
 */
public record VehicleOverviewResponse(
        long total,
        long available,
        long reserved,
        List<String> brands) {
}