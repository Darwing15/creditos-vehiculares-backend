package pe.sotomayor.creditos.shared.security;

/**
 * Proporciona la identidad del usuario autenticado
 * en el contexto actual de ejecución.
 *
 * <p>
 * Permite que los módulos funcionales conozcan al usuario
 * responsable de una operación sin depender directamente
 * de Spring Security ni de detalles internos del módulo
 * Security.
 * </p>
 *
 * @author DarwinG
 */
public interface CurrentUserProvider {

    /**
     * Obtiene el identificador interno del usuario autenticado.
     *
     * @return identificador del usuario autenticado.
     */
    Long getCurrentUserId();

}
