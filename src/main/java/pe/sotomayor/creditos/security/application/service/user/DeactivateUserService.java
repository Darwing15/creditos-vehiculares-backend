package pe.sotomayor.creditos.security.application.service.user;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;

import pe.sotomayor.creditos.security.application.mapper.user.UserMapper;

import pe.sotomayor.creditos.security.application.usecase.user.DeactivateUserUseCase;

import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

/**
 * Implementa el caso de uso para desactivar usuarios existentes.
 *
 * <p>
 * Realiza una desactivación funcional del usuario,
 * manteniendo la información histórica del registro.
 * </p>
 *
 * @author DarwinG
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeactivateUserService implements DeactivateUserUseCase {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    /**
     * Desactiva un usuario existente.
     *
     * @param id identificador del usuario
     * @return usuario desactivado
     */
    @Override
    public UserResponse deactivate(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                ErrorCode.USER_NOT_FOUND,
                                "User with id '%s' not found."
                                        .formatted(id)));

        if (!user.getActive()) {

            throw new ConflictException(
                    ErrorCode.USER_INACTIVE,
                    "User with id '%s is already inactive."
                            .formatted(id));
        }

        user.setActive(false);

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

}
