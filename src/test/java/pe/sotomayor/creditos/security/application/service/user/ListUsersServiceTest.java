package pe.sotomayor.creditos.security.application.service.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.sotomayor.creditos.security.application.dto.user.response.UserResponse;
import pe.sotomayor.creditos.security.application.mapper.user.UserMapper;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.domain.repository.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListUsersServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private ListUsersService service;

    @Test
    void shouldMapResultsWhenRecordsExist() throws Exception {
        /* Arrange */
        User entity = createUser();
        UserResponse expected = createResponse();
        when(userRepository.findAllByDeletedFalse()).thenReturn(List.of(entity));
        when(userMapper.toResponse(entity)).thenReturn(expected);

        /* Act */
        List<UserResponse> result = service.findAll();

        /* Assert */
        assertThat(result).containsExactly(expected);
        verify(userRepository).findAllByDeletedFalse();
        verify(userMapper).toResponse(entity);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    void shouldReturnEmptyListWhenNoRecordsExist() throws Exception {
        /* Arrange */
        when(userRepository.findAllByDeletedFalse()).thenReturn(List.of());

        /* Act */
        List<UserResponse> result = service.findAll();

        /* Assert */
        assertThat(result).isEmpty();
        verify(userRepository).findAllByDeletedFalse();
        verifyNoInteractions(userMapper);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    private User createUser() {
        return User.builder()
                .id(1L)
                .fullName("Ana Perez")
                .email("ana@test.com")
                .password("encoded-password")
                .active(true)
                .deleted(false)
                .tokenVersion(1L)
                .role(createRole())
                .branch(createBranch())
                .build();
    }

    private Role createRole() {
        return Role.builder()
                .id(2L)
                .name("Operador")
                .description("Consulta de usuarios")
                .permissions(new HashSet<>(Set.of("security:users:read")))
                .deleted(false)
                .build();
    }

    private Branch createBranch() {
        return Branch.builder()
                .id(3L)
                .code("LIM")
                .name("Lima")
                .address("Av. Peru 123")
                .email("lima@test.com")
                .phone("999999999")
                .active(true)
                .deleted(false)
                .build();
    }

    private UserResponse createResponse() {
        return new UserResponse(
                1L,
                "Ana Perez",
                "ana@test.com",
                2L,
                "Operador",
                3L,
                "Lima",
                true
        );
    }

}
