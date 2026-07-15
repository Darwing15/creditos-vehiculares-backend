package pe.sotomayor.creditos.security.application.service.user;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;

import pe.sotomayor.creditos.security.application.mapper.user.UserMapper;

import pe.sotomayor.creditos.security.application.usecase.user.ListUsersUseCase;

import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import java.util.List;

/**
 * Implementa el caso de uso para consultar usuarios registrados.
 *
 * <p>
 * Este servicio obtiene los usuarios almacenados y delega la transformación
 * de entidades de dominio a DTOs mediante {@link UserMapper}.
 * </p>
 *
 * <p>
 * La consulta no modifica información del sistema, por lo que la transacción
 * se mantiene únicamente en modo lectura.
 * </p>
 *
 * @author DarwinG
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListUsersService implements ListUsersUseCase {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    /**
     * Obtiene todos los usuarios registrados.
     *
     * @return lista de usuarios convertidos a objetos de respuesta
     */
    @Override
    public List<UserResponse> findAll() {

        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }
}
