package pe.sotomayor.creditos.customers.domain.model.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import pe.sotomayor.creditos.shared.persistence.BaseEntity;
import pe.sotomayor.creditos.customers.domain.model.valueobject.IdentityData;
import pe.sotomayor.creditos.customers.domain.exception.IdentityLookupException;

/**
 * Ultimo resultado de DNI obtenido; ciclo de vida independiente de la ficha comercial.
 *
 * @author DarwinG
 */
@Entity
@Table(name = "identity_document_cache")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdentityDocumentCache extends BaseEntity {

    @Column(
            name = "document_number",
            nullable = false,
            length = 8
    )
    private String documentNumber;

    @Column(
            name = "given_names",
            nullable = false,
            length = 150
    )
    private String givenNames;

    @Column(
            name = "first_surname",
            length = 150
    )
    private String firstSurname;

    @Column(
            name = "second_surname",
            length = 150
    )
    private String secondSurname;

    @Column(
            name = "full_name",
            nullable = false,
            length = 150
    )
    private String fullName;

    @Column(
            name = "provider",
            nullable = false,
            length = 30
    )
    private String provider;

    @Column(
            name = "consulted_at",
            nullable = false
    )
    private Instant consultedAt;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private Instant expiresAt;

    /**
     * @return resultado auditado y con caducidad estrictamente posterior a su obtencion.
     */
    public static IdentityDocumentCache create(
            IdentityData data,
            Instant consultedAt,
            Instant expiresAt,
            Long actorId) {
        if (data == null || consultedAt == null
                || expiresAt == null || !expiresAt.isAfter(consultedAt)
                || actorId == null || actorId <= 0)
            throw IdentityLookupException.invalidResponse();
        var entry = new IdentityDocumentCache();
        entry.documentNumber = data.documentNumber();
        entry.givenNames = data.givenNames();
        entry.firstSurname = data.firstSurname();
        entry.secondSurname = data.secondSurname();
        entry.fullName = data.fullName();
        entry.provider = data.provider();
        entry.consultedAt = consultedAt;
        entry.expiresAt = expiresAt;
        entry.setCreatedBy(actorId);
        entry.setUpdatedBy(actorId);
        entry.setCreatedAt(LocalDateTime.ofInstant(consultedAt, ZoneOffset.UTC));
        entry.setUpdatedAt(LocalDateTime.ofInstant(consultedAt, ZoneOffset.UTC));
        return entry;
    }

    /**
     * @return true unicamente cuando la vigencia supera el instante actual.
     */
    public boolean isFresh(Instant now) {
        return expiresAt.isAfter(now);
    }
}

