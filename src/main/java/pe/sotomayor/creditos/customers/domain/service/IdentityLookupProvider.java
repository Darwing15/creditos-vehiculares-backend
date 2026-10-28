package pe.sotomayor.creditos.customers.domain.service;

import pe.sotomayor.creditos.customers.domain.model.valueobject.IdentityData;

/**
 * Puerto de consulta externa, independiente del contrato HTTP del proveedor.
 *
 * @author DarwinG
 */
public interface IdentityLookupProvider {
    /**
     * @return datos minimos de un DNI ASCII de ocho digitos.
     */
    IdentityData lookup(String documentNumber);
}
