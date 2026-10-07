package pe.sotomayor.creditos.vehicles.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Datos editables y revision obtenida del detalle. No acepta identidad, sucursal ni estado.
 *
 * @author DarwinG
 */
public record UpdateVehicleRequest(

        @NotBlank
        @Size(max = 50)
        String vehicleType,

        @NotBlank
        @Size(max = 100)
        String brand,

        @NotBlank
        @Size(max = 150)
        String model,

        @NotNull
        Integer manufacturingYear,

        @NotBlank
        @Size(max = 50)
        String color,

        @NotBlank
        @Size(max = 100)
        String chassisNumber,

        @NotBlank
        @Size(max = 100)
        String engineNumber,

        @Size(max = 50)
        @Pattern(regexp = "(?s).*\\S.*")
        String duaNumber,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 10, fraction = 2)
        BigDecimal referencePrice,

        @NotBlank
        @Pattern(regexp = "[a-f0-9]{64}")
        String expectedRevision
) {
}
