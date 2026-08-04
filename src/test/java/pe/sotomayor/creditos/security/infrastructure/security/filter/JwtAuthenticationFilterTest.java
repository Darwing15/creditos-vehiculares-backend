package pe.sotomayor.creditos.security.infrastructure.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import pe.sotomayor.creditos.security.domain.model.entity.Branch;
import pe.sotomayor.creditos.security.domain.model.entity.Role;
import pe.sotomayor.creditos.security.domain.model.entity.User;

import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtService;
import pe.sotomayor.creditos.security.infrastructure.security.service.CustomUserDetailsService;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {

        response = new MockHttpServletResponse();

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {

        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateRequestWhenJwtIsValid()
            throws ServletException, IOException {

        /* Arrange */

        MockHttpServletRequest request = createRequest();

        User user = createUser();

        UserDetails userDetails = createUserDetails();

        when(jwtService.extractUsername("jwt-token"))
                .thenReturn(user.getEmail());

        when(jwtService.isTokenValid("jwt-token"))
                .thenReturn(true);

        when(jwtService.extractTokenVersion("jwt-token"))
                .thenReturn(user.getTokenVersion());

        when(userDetailsService.loadDomainUser(
                user.getEmail()))
                .thenReturn(user);

        when(userDetailsService.loadUserByUsername(
                user.getEmail()))
                .thenReturn(userDetails);

        /* Act */

        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain);

        /* Assert */

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        assertThat(authentication)
                .isNotNull();

        assertThat(authentication.getPrincipal())
                .isEqualTo(userDetails);

        assertThat(authentication.isAuthenticated())
                .isTrue();

        verify(filterChain).doFilter(request, response);

        verifyNoMoreInteractions(filterChain);
    }

    @Test
    void shouldContinueFilterChainWhenAuthorizationHeaderIsMissing()
            throws ServletException, IOException {

        /* Arrange */

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        /* Act */

        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain);

        /* Assert */

        assertThat(SecurityContextHolder.getContext()
                .getAuthentication())
                .isNull();

        verify(filterChain)
                .doFilter(request, response);

        verifyNoMoreInteractions(filterChain);
    }

    @Test
    void shouldContinueFilterChainWhenAuthorizationHeaderIsNotBearer()
            throws ServletException, IOException {

        /* Arrange */

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                "Authorization",
                "Basic dXNlcjpwYXNz");

        /* Act */

        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain);

        /* Assert */

        assertThat(SecurityContextHolder.getContext()
                .getAuthentication())
                .isNull();

        verify(filterChain)
                .doFilter(request, response);

        verifyNoMoreInteractions(filterChain);
    }

    @Test
    void shouldContinueFilterChainWhenUsernameIsNull()
            throws ServletException, IOException {

        /* Arrange */

        MockHttpServletRequest request =
                createRequest();

        when(jwtService.extractUsername("jwt-token"))
                .thenReturn(null);

        /* Act */

        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain);

        /* Assert */

        assertThat(SecurityContextHolder.getContext()
                .getAuthentication())
                .isNull();

        verify(filterChain)
                .doFilter(request, response);

        verifyNoMoreInteractions(filterChain);
    }

    @Test
    void shouldContinueFilterChainWhenAuthenticationAlreadyExists()
            throws ServletException, IOException {

        /* Arrange */

        MockHttpServletRequest request =
                createRequest();

        Authentication existingAuthentication =
                createAuthentication();

        SecurityContextHolder.getContext()
                .setAuthentication(existingAuthentication);

        when(jwtService.extractUsername("jwt-token"))
                .thenReturn("darwin@creditos.com");

        /* Act */

        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain);

        /* Assert */

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        assertThat(authentication)
                .isSameAs(existingAuthentication);

        verifyNoInteractions(userDetailsService);

        verify(filterChain)
                .doFilter(request, response);

        verifyNoMoreInteractions(filterChain);
    }

    @Test
    void shouldContinueFilterChainWhenTokenIsInvalid()
            throws ServletException, IOException {

        /* Arrange */

        MockHttpServletRequest request =
                createRequest();

        when(jwtService.extractUsername("jwt-token"))
                .thenReturn("darwin@creditos.com");

        when(jwtService.isTokenValid("jwt-token"))
                .thenReturn(false);

        /* Act */

        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain);

        /* Assert */

        assertThat(SecurityContextHolder.getContext()
                .getAuthentication())
                .isNull();

        verify(filterChain)
                .doFilter(request, response);

        verifyNoMoreInteractions(filterChain);
    }

    @Test
    void shouldNotAuthenticateWhenTokenVersionDoesNotMatch()
            throws ServletException, IOException {

        /* Arrange */

        MockHttpServletRequest request =
                createRequest();

        User user = createUser();

        when(jwtService.extractUsername("jwt-token"))
                .thenReturn(user.getEmail());

        when(jwtService.isTokenValid("jwt-token"))
                .thenReturn(true);

        when(jwtService.extractTokenVersion("jwt-token"))
                .thenReturn(99L);

        when(userDetailsService.loadDomainUser(user.getEmail()))
                .thenReturn(user);

        /* Act */

        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain);

        /* Assert */

        assertThat(SecurityContextHolder.getContext()
                .getAuthentication())
                .isNull();

        verify(userDetailsService).
                loadDomainUser(user.getEmail());

        verify(filterChain)
                .doFilter(request, response);

        verifyNoMoreInteractions(
                userDetailsService,
                filterChain);
    }

    @Test
    void shouldClearSecurityContextWhenAuthenticationFails()
            throws ServletException, IOException {

        /* Arrange */

        MockHttpServletRequest request = createRequest();

        SecurityContextHolder.getContext()
                .setAuthentication(createAuthentication());

        when(jwtService.extractUsername("jwt-token"))
                .thenThrow(new RuntimeException("JWT error"));

        /* Act */

        jwtAuthenticationFilter.doFilterInternal(
                request,
                response,
                filterChain);

        /* Assert */

        assertThat(SecurityContextHolder.getContext()
                .getAuthentication())
                .isNull();

        verify(filterChain)
                .doFilter(request, response);

        verifyNoMoreInteractions(filterChain);
    }

    private MockHttpServletRequest createRequest() {
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                "Authorization",
                "Bearer jwt-token");

        return request;
    }

    private Authentication createAuthentication() {

        return new UsernamePasswordAuthenticationToken(
                "darwin@creditos.com",
                null,
                Collections.emptyList()
        );
    }

    private UserDetails createUserDetails() {

        return org.springframework.security.core.userdetails.User
                .withUsername("darwin@creditos.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();
    }

    private User createUser() {

        User user = new User();

        user.setId(1L);
        user.setFullName("Darwin G.");
        user.setEmail("darwin@creditos.com");
        user.setPassword("$2a$10$passwordHash");
        user.setRole(createRole());
        user.setBranch(createBranch());
        user.setActive(true);
        user.setTokenVersion(1L);

        return user;
    }

    private Role createRole() {

        Role role = new Role();

        role.setId(1L);
        role.setName("Administrador");

        return role;
    }

    private Branch createBranch() {

        Branch branch = new Branch();

        branch.setId(1L);
        branch.setName("Sede Tocache");

        return branch;
    }
}
