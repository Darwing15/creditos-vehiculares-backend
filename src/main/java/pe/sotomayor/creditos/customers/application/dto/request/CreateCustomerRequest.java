package pe.sotomayor.creditos.customers.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import pe.sotomayor.creditos.customers.domain.model.enumeration.CustomerDocumentType;

import java.math.BigDecimal;

/**
 * Datos proporcionados para registrar un cliente, conforme al esquema V3.
 * La identidad publica, sucursal y estado inicial son determinados por el sistema.
 * Este contrato es utilizado por el caso de uso y el endpoint de alta de clientes.
 *
 * @param fullName       nombre completo
 * @param documentType   tipo de documento
 * @param documentNumber numero de documento
 * @param phone          telefono opcional
 * @param email          correo opcional
 * @param occupation     ocupacion opcional
 * @param monthlyIncome  ingreso mensual declarado, opcional y no negativo
 * @param address        direccion opcional
 * @param mapReference   referencia de ubicacion opcional
 * @author DarwinG
 */
public record CreateCustomerRequest(

        @NotBlank
        @Size(max = 150)
        String fullName,

        @NotNull
        CustomerDocumentType documentType,

        @NotBlank
        @Size(max = 20)
        String documentNumber,

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
        String mapReference
) {
}

