package pe.sotomayor.creditos.vehicles.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Datos de una unidad fisica conforme al esquema V4.
 * No recibe id, UUID, sucursal, auditoria ni estado del navegador.
 * Una unidad nueva nace AVAILABLE y su reserva utiliza la operacion de dominio.
 *
 * @param vehicleType       tipo comercial
 * @param brand             marca
 * @param model             modelo
 * @param manufacturingYear anio de fabricacion
 * @param color             color
 * @param chassisNumber     identificador de chasis
 * @param engineNumber      identificador de motor
 * @param duaNumber         DUA opcional, no vacia si se informa
 * @param referencePrice    precio de referencia positivo
 * @author DarwinG
 */
public record CreateVehicleRequest(

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
        @Pattern(regexp = "(?s).*\\S.*", message = "La DUA no puede estar vacía.")
        String duaNumber,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 10, fraction = 2)
        BigDecimal referencePrice
) {
}

