package pe.sotomayor.creditos.security.application.service.user;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.sotomayor.creditos.security.application.dto.user.response.UserFilterOption;
import pe.sotomayor.creditos.security.application.dto.user.response.UserFilterOptions;
import pe.sotomayor.creditos.security.application.dto.user.response.UserPageResponse;
import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;

import pe.sotomayor.creditos.security.application.mapper.user.UserMapper;
import pe.sotomayor.creditos.security.application.usecase.user.ListUsersUseCase;

import pe.sotomayor.creditos.security.domain.repository.UserRepository;
import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ValidationException;

import java.util.List;

/**
 * Implementa el caso de uso para consultar usuarios registrados.
 *
 * <p>
 * Este servicio obtiene los usuarios almacenados y delega la transformacion
 * de entidades de dominio a DTOs mediante {@link UserMapper}.
 * </p>
 *
 * <p>
 * La consulta no modifica informacion del sistema, por lo que la transaccion
 * se mantiene unicamente en modo lectura.
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
    @PreAuthorize("hasAuthority('security:users:read')")
    public List<UserResponse> findAll() {

        return userRepository.findAllByDeletedFalse()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasAuthority('security:users:read')")
    public UserPageResponse searchPage(
            String query, Long roleId, Long branchId, int page, int size) {

        if (page < 0 || page > 1_000_000 || size < 1 || size > 100
                || (query != null && query.length() > 150)
                || (roleId != null && roleId <= 0) || (branchId != null && branchId <= 0)) {

            throw new ValidationException(ErrorCode.VALIDATION_ERROR,
                    "La búsqueda o los parámetros de paginación son inválidos.");
        }
        var result = userRepository.searchPage(
                query == null ? "" : query.strip(),
                roleId == null ? 0L : roleId,
                branchId == null ? 0L : branchId,
                PageRequest.of(page, size, Sort.by("id")
                        .ascending()));

        return new UserPageResponse(
                result.getContent().stream().map(userMapper::toResponse).toList(),
                result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PreAuthorize("hasAuthority('security:users:read')")
    public UserFilterOptions filterOptions() {
        return new UserFilterOptions(toOptions(
                userRepository.findVisibleRoleOptions()),
                toOptions(userRepository.findVisibleBranchOptions()));
    }

    /**
     * Convierte las filas de identificador y nombre del repositorio en opciones.
     *
     * @param rows filas con identificador Long y nombre String, en ese orden
     * @return opciones del filtro
     */
    private List<UserFilterOption> toOptions(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new UserFilterOption((Long) row[0], (String) row[1]))
                .toList();
    }
}
