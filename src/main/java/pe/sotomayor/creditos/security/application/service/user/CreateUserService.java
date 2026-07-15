package pe.sotomayor.creditos.security.application.service.user;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.user.request.CreateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;

import pe.sotomayor.creditos.security.application.mapper.user.UserMapper;

import pe.sotomayor.creditos.security.application.usecase.user.CreateUserUseCase;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.domain.repository.BranchRepository;
import pe.sotomayor.creditos.security.domain.repository.RoleRepository;
import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;
import pe.sotomayor.creditos.shared.exception.ResourceNotFoundException;

/**
 * Implementa el caso de uso para registrar nuevos usuarios.
 *
 * <p>
 * Coordina las validaciones de negocio necesarias para crear un usuario,
 * incluyendo la validación de correo, existencia de rol y sucursal,
 * asignación de relaciones del usuario y cifrado de contraseña.
 * </p>
 *
 * @author DarwinG
 */

@RequiredArgsConstructor
@Service
@Transactional
public class CreateUserService implements CreateUserUseCase {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final BranchRepository branchRepository;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    /**
     * Registra un nuevo usuario.
     *
     * @param request información requerida para crear el usuario
     * @return información del usuario creado
     */
    @Override
    public UserResponse create(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException(
                    ErrorCode.USER_ALREADY_EXISTS,
                    "User with email '%s' already exists."
                            .formatted(request.email()));
        }

        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                ErrorCode.ROLE_NOT_FOUND,
                                "Role with id '%s' not found."
                                        .formatted(request.roleId())));

        Branch branch = branchRepository.findById(request.branchId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                ErrorCode.BRANCH_NOT_FOUND,
                                "Branch with id '%s' not found."
                                        .formatted(request.branchId())));

        User user = userMapper.toEntity(request);

        user.setRole(role);

        user.setBranch(branch);

        user.setPassword(
                passwordEncoder.encode(request.password())
        );

        user.setActive(true);

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

}