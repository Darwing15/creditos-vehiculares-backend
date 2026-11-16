package pe.sotomayor.creditos.credit.application.dto.creditapplication.response;

import java.util.List;

public record CreditApplicationPageResponse(
        List<CreditApplicationPageItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
