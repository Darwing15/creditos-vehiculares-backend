package pe.sotomayor.creditos.customers.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.SQLRestriction;

import pe.sotomayor.creditos.customers.domain.model.enumeration.CustomerDocumentType;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ValidationException;
import pe.sotomayor.creditos.shared.persistence.SoftDeleteEntity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Agregado de cliente persistido conforme a V3; conserva identidad y datos de contacto.
 *
 * @author DarwinG
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "customers")
@SQLRestriction("is_deleted = false")
public class Customer extends SoftDeleteEntity {
    @Column(
            name = "customer_code",
            nullable = false,
            unique = true
    )
    private UUID customerCode;

    @Column(
            name = "full_name",
            nullable = false,
            length = 150
    )
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "document_type",
            nullable = false,
            length = 10
    )
    private CustomerDocumentType documentType;

    @Column(
            name = "document_number",
            nullable = false,
            length = 20
    )
    private String documentNumber;

    @Column(length = 20)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(length = 150)
    private String occupation;

    @Column(
            name = "monthly_income",
            precision = 12,
            scale = 2
    )
    private BigDecimal monthlyIncome;

    @Column(length = 255)
    private String address;

    @Column(
            name = "map_reference",
            length = 255
    )
    private String mapReference;

    @Column(
            name = "branch_id",
            nullable = false
    )
    private Long branchId;

    @Column(
            name = "is_active",
            nullable = false
    )
    private Boolean active;

    /**
     * Crea un cliente activo con los datos de registro y el contexto del sistema.
     * No recibe un DTO de Application ni un identificador de persistencia.
     * Las validaciones de unicidad y sucursal existente pertenecen al futuro caso de uso.
     *
     * @param customerCode   UUID generado por el sistema
     * @param fullName       nombre completo
     * @param documentType   tipo de documento
     * @param documentNumber numero de documento
     * @param phone          telefono opcional
     * @param email          correo opcional, validado en el contrato de entrada
     * @param occupation     ocupacion opcional
     * @param monthlyIncome  ingreso mensual declarado opcional
     * @param address        direccion opcional
     * @param mapReference   referencia de ubicacion opcional
     * @param branchId       sucursal obtenida del contexto autenticado
     * @return cliente activo y no eliminado, aun sin id ni auditoria de persistencia
     */
    public static Customer create(
            UUID customerCode,
            String fullName,
            CustomerDocumentType documentType,
            String documentNumber,
            String phone,
            String email,
            String occupation,
            BigDecimal monthlyIncome,
            String address,
            String mapReference,
            Long branchId) {

        validate(customerCode != null, "Customer code is required");
        validate(documentType != null, "Document type is required");
        validate(branchId != null && branchId > 0, "Branch id must be positive");
        validateRequiredText(fullName, 150, "Full name");
        validateRequiredText(documentNumber, 20, "Document number");
        validateOptionalText(phone, 20, "Phone");
        validateOptionalText(email, 120, "Email");
        validateOptionalText(occupation, 150, "Occupation");
        validateOptionalText(address, 255, "Address");
        validateOptionalText(mapReference, 255, "Map reference");
        if (monthlyIncome != null) {
            validate(monthlyIncome.signum() >= 0, "Monthly income cannot be negative");
            validate(monthlyIncome.scale() <= 2 && monthlyIncome.precision() - monthlyIncome.scale() <= 10,
                    "Monthly income must fit NUMERIC(12,2)");
        }

        Customer customer = new Customer();
        customer.customerCode = customerCode;
        customer.fullName = fullName;
        customer.documentType = documentType;
        customer.documentNumber = documentNumber;
        customer.phone = phone;
        customer.email = email;
        customer.occupation = occupation;
        customer.monthlyIncome = monthlyIncome;
        customer.address = address;
        customer.mapReference = mapReference;
        customer.branchId = branchId;
        customer.active = true;
        return customer;
    }

    /**
     * Valida los textos obligatorios sin alterar su contenido.
     */
    private static void validateRequiredText(
            String value,
            int maxLength,
            String field) {
        validate(
                value != null
                        && !value.isBlank()
                        && value.length() <= maxLength,
                field + " is required and must not exceed " + maxLength + " characters");
    }

    /**
     * Valida la longitud de los textos opcionales.
     */
    private static void validateOptionalText(
            String value,
            int maxLength,
            String field) {
        validate(value == null || value.length() <= maxLength,
                field + " must not exceed " + maxLength + " characters");
    }

    /**
     * Rechaza datos de construccion que no cumplen las restricciones de registro.
     */
    private static void validate(boolean valid, String message) {
        if (!valid) {
            throw new ValidationException(ErrorCode.VALIDATION_ERROR, message);
        }
    }

    /**
     * Actualiza la ficha del mismo cliente; conserva documento, identidad, sucursal y estado.
     */
    public void updateProfile(
            String fullName,
            String phone,
            String email,
            String occupation,
            BigDecimal monthlyIncome,
            String address,
            String mapReference) {

        Customer validated = create(
                customerCode,
                fullName,
                documentType,
                documentNumber,
                phone,
                email,
                occupation,
                monthlyIncome,
                address,
                mapReference,
                branchId
        );
        this.fullName = validated.fullName;
        this.phone = validated.phone;
        this.email = validated.email;
        this.occupation = validated.occupation;
        this.monthlyIncome = validated.monthlyIncome;
        this.address = validated.address;
        this.mapReference = validated.mapReference;
    }
}
