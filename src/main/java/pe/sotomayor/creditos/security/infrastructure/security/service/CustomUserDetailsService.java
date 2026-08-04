package pe.sotomayor.creditos.security.infrastructure.security.service;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;

import pe.sotomayor.creditos.security.domain.model.entity.User;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import java.util.List;

/**
 * Implementación de {@link UserDetailsService} utilizada por Spring Security.
 *
 * <p>
 * Se encarga de recuperar la información de autenticación de un usuario
 * a partir de su correo electrónico, adaptando la entidad del dominio
 * al contrato requerido por Spring Security.
 * </p>
 *
 * <p>
 * Este servicio constituye el punto de integración entre el modelo de
 * dominio y el mecanismo de autenticación del framework.
 * </p>
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Obtiene la información de autenticación del usuario.
     *
     * @param email correo electrónico del usuario
     * @return información utilizada por Spring Security
     * @throws UsernameNotFoundException si el usuario no existe
     */
    @Override
    public UserDetails loadUserByUsername(String email) {

        User user = userRepository.findUserWithRoleAndBranchByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + email));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().getName().toUpperCase()
                        )))
                .disabled(!Boolean.TRUE.equals(user.getActive()))
                .build();
    }

    /**
     * Obtiene la entidad de dominio correspondiente al usuario
     * autenticado.
     *
     * <p>
     * Este método está destinado exclusivamente a la infraestructura
     * de seguridad cuando necesita acceder a información adicional
     * del usuario que no forma parte del contrato de
     * {@link org.springframework.security.core.userdetails.UserDetails}.
     * </p>
     *
     * <p>
     * La consulta recupera anticipadamente las asociaciones necesarias
     * para evitar problemas de inicialización diferida durante el
     * procesamiento del filtro JWT.
     * </p>
     *
     * @param email correo electrónico del usuario
     * @return entidad de dominio completamente inicializada
     * @throws UsernameNotFoundException si el usuario no existe
     */
    public User loadDomainUser(String email) {

        return userRepository
                .findUserWithRoleAndBranchByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found: " + email));
    }

}
