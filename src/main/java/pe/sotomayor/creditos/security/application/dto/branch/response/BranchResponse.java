package pe.sotomayor.creditos.security.application.dto.branch.response;

/**
 * Información pública de una sucursal del sistema.
 *
 * <p>
 * Representa los datos utilizados por los casos de uso
 * de consulta y administración de sucursales.
 * </p>
 *
 * @param id        identificador único de la sucursal
 * @param code      código operativo de la sucursal
 * @param name      nombre descriptivo de la sucursal
 * @param address   dirección física de la sucursal
 * @param phone     teléfono de contacto
 * @param email     correo electrónico de contacto
 * @param managerId identificador del usuario responsable
 * @param active    estado operativo de la sucursal
 * @author DarwinG
 */
public record BranchResponse(

        Long id,

        String code,

        String name,

        String address,

        String phone,

        String email,

        Long managerId,

        Boolean active

) {
}
