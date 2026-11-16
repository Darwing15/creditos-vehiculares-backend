package pe.sotomayor.creditos.vehicles.application.dto.response;

import java.util.List;

public record VehiclePageResponse(
        List<VehicleOptionResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}