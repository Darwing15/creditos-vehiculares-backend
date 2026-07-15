package pe.sotomayor.creditos.security.application.service.user;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.user.request.UpdateUserRequest;
import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;

import pe.sotomayor.creditos.security.application.mapper.user.UserMapper;
import pe.sotomayor.creditos.security.application.usecase.user.UpdateUserUseCase;

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
 * Implementa el caso de uso para actualizar usuarios existentes.
 *
 * <p>
 * Gestiona la actualización de información administrativa del usuario,
 * incluyendo validación de correo, existencia de rol y sucursal.
 * </p>
 *
 * @author DarwinG
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateUserService implements UpdateUserUseCase {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final BranchRepository branchRepository;

    private final UserMapper userMapper;

    /**
     * Actualiza un usuario existente.
     *
     * @param id      identificador del usuario
     * @param request información actualizada
     * @return usuario actualizado
     */
    @Override
    public UserResponse update(Long id, UpdateUserRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                ErrorCode.USER_NOT_FOUND,
                                "User with id '%s' not found."
                                        .formatted(id)));

        if (userRepository.existsByEmailAndIdNot(request.email(), id)) {

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

        userMapper.updateEntity(request, user);

        user.setRole(role);

        user.setBranch(branch);

        User updatedUser = userRepository.save(user);

        return userMapper.toResponse(updatedUser);
    }

}
