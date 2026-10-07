package pe.sotomayor.creditos.security.infrastructure.security.service;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;

import java.util.Locale;

/**
 * Adapta la cuenta y los permisos persistidos al contrato UserDetailsService.
 *
 * <p>Reemplaza la autoridad construida desde el nombre del rol y evalúa los
 * estados de todas las relaciones. La transacción permite copiar datos lazy
 * antes de salir del servicio, sin depender de Open Session in View.</p>
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    /**
     * Acceso de lectura a cuentas y relaciones requeridas por autenticación.
     */
    private final UserRepository userRepository;

    /**
     * Carga el principal para comprobar las credenciales de inicio de sesión.
     *
     * @param email correo recibido por el proveedor de autenticación
     * @return principal cuyo enabled refleja el estado agregado de acceso
     * @throws UsernameNotFoundException si no hay cuenta o faltan relaciones requeridas
     */
    @Override
    public SecurityPrincipal loadUserByUsername(String email) {

        if (email == null) {
            throw new UsernameNotFoundException("Credenciales inválidas.");
        }

        return principal(userRepository.findForAuthenticationByEmail(
                        email.strip().toLowerCase(Locale.ROOT))
                .orElseThrow(() ->
                        new UsernameNotFoundException("Credenciales inválidas.")));
    }

    /**
     * Carga el principal para una solicitud identificada por ID de usuario.
     *
     * @param id identidad estable previamente extraída del JWT válido
     * @return principal con estados y permisos de la lectura actual
     * @throws UsernameNotFoundException si no hay cuenta o faltan relaciones requeridas
     */
    public SecurityPrincipal loadUserById(Long id) {
        return principal(userRepository.findForAuthenticationById(id)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Credenciales inválidas.")));
    }

    /**
     * Copia el estado del dominio a un principal sin exponer entidades JPA.
     *
     * @param user cuenta obtenida por el repositorio con sus relaciones cargadas
     * @return principal habilitado únicamente cuando todos los estados lo permiten
     * @throws UsernameNotFoundException si rol o sucursal están ausentes
     */
    private SecurityPrincipal principal(User user) {
        /* Relaciones que condicionan disponibilidad y datos de presentación. */
        var role = user.getRole();
        var branch = user.getBranch();
        if (role == null || branch == null) {
            throw new UsernameNotFoundException("Credenciales inválidas.");
        }
        /* Relaciones que condicionan disponibilidad y datos de presentación. */
        boolean enabled = Boolean.TRUE.equals(user.getActive())
                && Boolean.FALSE.equals(user.getDeleted())
                && Boolean.FALSE.equals(role.getDeleted())
                && Boolean.TRUE.equals(branch.getActive())
                && Boolean.FALSE.equals(branch.getDeleted())
                && user.getTokenVersion() != null && user.getTokenVersion() > 0;
        /* Los permisos son códigos persistidos; el nombre del rol no interviene. */
        var authorities = role.getPermissions().stream().sorted()
                .map(SimpleGrantedAuthority::new).toList();

        return new SecurityPrincipal(
                user.getId(), user.getTokenVersion(), user.getFullName(),
                user.getEmail(), user.getPassword(), role.getId(), role.getName(),
                branch.getId(), branch.getName(), enabled, authorities);
    }

}
