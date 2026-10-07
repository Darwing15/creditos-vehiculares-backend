package pe.sotomayor.creditos.security.infrastructure.security.filter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import pe.sotomayor.creditos.security.infrastructure.security.config.SecurityErrorHandler;
import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtService;
import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;
import pe.sotomayor.creditos.security.infrastructure.security.service.CustomUserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwt;

    @Mock
    private CustomUserDetailsService users;

    @Mock
    private SecurityErrorHandler errors;

    @Mock
    private FilterChain chain;

    @InjectMocks
    private JwtAuthenticationFilter service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueAnonymouslyWhenHeaderIsAbsent() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        /* Act */
        service.doFilter(request, response, chain);

        /* Assert */
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwt, users, errors);
        verifyNoMoreInteractions(chain, jwt, users, errors);
    }

    @Test
    void shouldSkipTokenWhenRequestIsPreflight() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("OPTIONS");
        request.setServletPath("/api/auth/me");
        request.addHeader("Authorization", "invalid");

        /* Act */
        service.doFilter(request, response, chain);

        /* Assert */
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwt, users, errors);
        verifyNoMoreInteractions(chain, jwt, users, errors);
    }

    @Test
    void shouldSkipTokenWhenRequestIsLogin() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setMethod("POST");
        request.setServletPath("/api/auth/login");
        request.addHeader("Authorization", "invalid");

        /* Act */
        service.doFilter(request, response, chain);

        /* Assert */
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwt, users, errors);
        verifyNoMoreInteractions(chain, jwt, users, errors);
    }

    @Test
    void shouldAuthenticateWithFreshAuthoritiesWhenTokenIsValid() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "bEaReR signed-token");
        SecurityPrincipal principal = createPrincipal();
        when(jwt.parseAccessToken("signed-token")).thenReturn(new JwtService.AccessToken(1L, 1L));
        when(users.loadUserById(1L)).thenReturn(principal);

        /* Act */
        service.doFilter(request, response, chain);

        /* Assert */
        Authentication result = SecurityContextHolder.getContext().getAuthentication();
        assertThat(result).isNotNull();
        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isSameAs(principal);
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("security:users:read");
        assertThat(principal.getPassword()).isNull();
        assertThat(result.getCredentials()).isNull();
        verify(jwt).parseAccessToken("signed-token");
        verify(users).loadUserById(1L);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(errors);
        verifyNoMoreInteractions(chain, jwt, users, errors);
    }

    @Test
    void shouldRejectHeaderWhenSchemeIsWrong() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Basic abc");
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(createPrincipal()));

        /* Act */
        service.doFilter(request, response, chain);

        /* Assert */
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(errors).commence(eq(request), eq(response), any(BadCredentialsException.class));
        verifyNoInteractions(jwt, users, chain);
        verifyNoMoreInteractions(chain, jwt, users, errors);
    }

    @Test
    void shouldRejectHeaderWhenTokenIsEmpty() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer ");
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(createPrincipal()));

        /* Act */
        service.doFilter(request, response, chain);

        /* Assert */
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(errors).commence(eq(request), eq(response), any(BadCredentialsException.class));
        verifyNoInteractions(jwt, users, chain);
        verifyNoMoreInteractions(chain, jwt, users, errors);
    }

    @Test
    void shouldRejectHeaderWhenTokenHasWhitespace() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer a b");
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(createPrincipal()));

        /* Act */
        service.doFilter(request, response, chain);

        /* Assert */
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(errors).commence(eq(request), eq(response), any(BadCredentialsException.class));
        verifyNoInteractions(jwt, users, chain);
        verifyNoMoreInteractions(chain, jwt, users, errors);
    }

    @Test
    void shouldRejectHeaderWhenHeadersAreDuplicated() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer one");
        request.addHeader("Authorization", "Bearer two");
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(createPrincipal()));

        /* Act */
        service.doFilter(request, response, chain);

        /* Assert */
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(errors).commence(eq(request), eq(response), any(BadCredentialsException.class));
        verifyNoInteractions(jwt, users, chain);
        verifyNoMoreInteractions(chain, jwt, users, errors);
    }

    @Test
    void shouldRejectHeaderWhenHeaderIsTooLong() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer " + "a".repeat(8192));
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(createPrincipal()));

        /* Act */
        service.doFilter(request, response, chain);

        /* Assert */
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(errors).commence(eq(request), eq(response), any(BadCredentialsException.class));
        verifyNoInteractions(jwt, users, chain);
        verifyNoMoreInteractions(chain, jwt, users, errors);
    }

    @Test
    void shouldStopAuthenticationWhenMalformed() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer signed-token");
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(createPrincipal()));
        when(jwt.parseAccessToken("signed-token")).thenThrow(new JwtException("invalid"));

        /* Act */
        service.doFilter(request, response, chain);

        /* Assert */
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwt).parseAccessToken("signed-token");
        verifyNoInteractions(users);
        verify(errors).commence(eq(request), eq(response), any(BadCredentialsException.class));
        verifyNoInteractions(chain);
        verifyNoMoreInteractions(chain, jwt, users, errors);
    }

    @Test
    void shouldStopAuthenticationWhenRevoked() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer signed-token");
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(createPrincipal()));
        when(jwt.parseAccessToken("signed-token")).thenReturn(new JwtService.AccessToken(1L, 2L));
        when(users.loadUserById(1L)).thenReturn(createPrincipal());

        /* Act */
        service.doFilter(request, response, chain);

        /* Assert */
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwt).parseAccessToken("signed-token");
        verify(users).loadUserById(1L);
        verify(errors).commence(eq(request), eq(response), any(BadCredentialsException.class));
        verifyNoInteractions(chain);
        verifyNoMoreInteractions(chain, jwt, users, errors);
    }

    @Test
    void shouldStopAuthenticationWhenDisabled() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer signed-token");
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(createPrincipal()));
        when(jwt.parseAccessToken("signed-token")).thenReturn(new JwtService.AccessToken(1L, 1L));
        SecurityPrincipal valid = createPrincipal();
        SecurityPrincipal principal = new SecurityPrincipal(
                1L,
                1L,
                valid.getFullName(),
                valid.getUsername(),
                valid.getPassword(),
                2L,
                "Operador",
                3L,
                "Lima",
                false,
                valid.getAuthorities()
        );
        when(users.loadUserById(1L)).thenReturn(principal);

        /* Act */
        service.doFilter(request, response, chain);

        /* Assert */
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwt).parseAccessToken("signed-token");
        verify(users).loadUserById(1L);
        verify(errors).commence(eq(request), eq(response), any(BadCredentialsException.class));
        verifyNoInteractions(chain);
        verifyNoMoreInteractions(chain, jwt, users, errors);
    }

    @Test
    void shouldStopAuthenticationWhenUserMissing() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer signed-token");
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(createPrincipal()));
        when(jwt.parseAccessToken("signed-token")).thenReturn(new JwtService.AccessToken(1L, 1L));
        when(users.loadUserById(1L)).thenThrow(new UsernameNotFoundException("missing"));

        /* Act */
        service.doFilter(request, response, chain);

        /* Assert */
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwt).parseAccessToken("signed-token");
        verify(users).loadUserById(1L);
        verify(errors).commence(eq(request), eq(response), any(BadCredentialsException.class));
        verifyNoInteractions(chain);
        verifyNoMoreInteractions(chain, jwt, users, errors);
    }

    @Test
    void shouldStopAuthenticationWhenDatabaseUnavailable() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer signed-token");
        SecurityContextHolder.getContext().setAuthentication(createAuthentication(createPrincipal()));
        when(jwt.parseAccessToken("signed-token")).thenReturn(new JwtService.AccessToken(1L, 1L));
        when(users.loadUserById(1L)).thenThrow(new DataAccessResourceFailureException("offline"));

        /* Act */
        service.doFilter(request, response, chain);

        /* Assert */
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwt).parseAccessToken("signed-token");
        verify(users).loadUserById(1L);
        verify(errors).unavailable(request, response);
        verifyNoInteractions(chain);
        verifyNoMoreInteractions(chain, jwt, users, errors);
    }

    @Test
    void shouldPropagateDownstreamFailureWhenAuthenticationAlreadySucceeded() throws Exception {
        /* Arrange */
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = createRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer signed-token");
        when(jwt.parseAccessToken("signed-token")).thenReturn(new JwtService.AccessToken(1L, 1L));
        when(users.loadUserById(1L)).thenReturn(createPrincipal());
        ServletException failure = new ServletException("controller failure");
        doThrow(failure).when(chain).doFilter(request, response);

        /* Act */
        Throwable thrown = catchThrowable(() -> service.doFilter(request, response, chain));

        /* Assert */
        assertThat(thrown).isSameAs(failure);
        verify(jwt).parseAccessToken("signed-token");
        verify(users).loadUserById(1L);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(errors);
        verifyNoMoreInteractions(chain, jwt, users, errors);
    }

    private SecurityPrincipal createPrincipal() {
        return new SecurityPrincipal(
                1L,
                1L,
                "Ana Perez",
                "ana@test.com",
                "encoded-password",
                2L,
                "Operador",
                3L,
                "Lima",
                true,
                List.of(new SimpleGrantedAuthority("security:users:read"))
        );
    }

    private MockHttpServletRequest createRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.setServletPath("/api/auth/me");
        return request;
    }

    private Authentication createAuthentication(SecurityPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
