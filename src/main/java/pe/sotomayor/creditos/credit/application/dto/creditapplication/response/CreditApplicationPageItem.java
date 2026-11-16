package pe.sotomayor.creditos.credit.application.dto.creditapplication.response;

/**
 * Los nombres son opcionales y estan sujetos a los permisos de lectura correspondientes.
 *
 * @author DarwinG
 */
public record CreditApplicationPageItem(
        CreditApplicationResponse application,
        String customerName,
        String vehicleName) {
}
