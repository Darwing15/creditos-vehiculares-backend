package pe.sotomayor.creditos.customers.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Datos editables y revision obtenida del detalle. No acepta identidad, sucursal ni estado.
 *
 * @author DarwinG
 */
public record UpdateCustomerRequest(

        @NotBlank
        @Size(max = 150)
        String fullName,

        @Size(max = 20)
        String phone,

        @Email
        @Size(max = 120)
        String email,

        @Size(max = 150)
        String occupation,

        @DecimalMin("0.00")
        @Digits(integer = 10, fraction = 2)
        BigDecimal monthlyIncome,

        @Size(max = 255)
        String address,

        @Size(max = 255)
        String mapReference,

        @NotBlank
        @Pattern(regexp = "[a-f0-9]{64}")
        String expectedRevision
) {
}
