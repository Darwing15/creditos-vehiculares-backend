package pe.sotomayor.creditos.customers.domain.model.valueobject;

import java.util.stream.Stream;
import java.util.stream.Collectors;

import pe.sotomayor.creditos.customers.domain.exception.IdentityLookupException;

/**
 * Datos minimos normalizados de una consulta; no representan una verificacion biometrica.
 *
 * @author DarwinG
 */
public record IdentityData(
        String documentNumber,
        String givenNames,
        String firstSurname,
        String secondSurname,
        String provider) {

    /**
     * Valida el documento y preserva nombres compuestos y signos propios del nombre.
     */
    public IdentityData {
        if (documentNumber == null || !documentNumber.matches("[0-9]{8}"))
            throw IdentityLookupException.invalidResponse();
        givenNames = normalize(givenNames);
        firstSurname = normalize(firstSurname);
        secondSurname = normalize(secondSurname);
        if (givenNames == null || provider == null || !provider.equals("FACTILIZA"))
            throw IdentityLookupException.invalidResponse();
        if (compose(givenNames, firstSurname, secondSurname).length() > 150)
            throw IdentityLookupException.invalidResponse();
    }

    /**
     * @return nombres seguidos de apellidos, sin separadores inventados.
     */
    public String fullName() {
        return compose(givenNames, firstSurname, secondSurname);
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String text = value.strip().replaceAll("\\s+", " ");
        if (text.isEmpty()) return null;
        if (text.length() > 150 || text.codePoints().anyMatch(Character::isISOControl))
            throw IdentityLookupException.invalidResponse();
        return text;
    }

    private static String compose(String names, String first, String second) {
        return Stream.of(names, first, second).filter(v -> v != null && !v.isBlank())
                .collect(Collectors.joining(" "));
    }
}

