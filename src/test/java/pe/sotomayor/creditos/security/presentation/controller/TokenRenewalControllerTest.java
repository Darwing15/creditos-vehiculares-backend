package pe.sotomayor.creditos.security.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.JwtException;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;

import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.sotomayor.creditos.security.infrastructure.security.config.SecurityConfig;
import pe.sotomayor.creditos.security.infrastructure.security.config.SecurityErrorHandler;
import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtService;
import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;
import pe.sotomayor.creditos.security.infrastructure.security.service.CustomUserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(TokenRenewalController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class})
@TestPropertySource(properties = {"app.security.allowed-origins=http://localhost:5173", "app.security.docs-enabled=false"})
class TokenRenewalControllerTest {
    @Autowired
    MockMvc mvc;
    @MockitoBean
    JwtService jwt;
    @MockitoBean
    CustomUserDetailsService users;

    private SecurityPrincipal principal(long version) {
        return new SecurityPrincipal(
                1L,
                version,
                "Usuario",
                "a@b.pe",
                "hash",
                1L,
                "rol",
                1L,
                "sucursal",
                true,
                List.of()
        );
    }

    @Test
    void requiresAuthentication() throws Exception {

        /* Arrange */

        var request = post("/api/auth/renew");

        /* Act */

        var result = mvc.perform(request).andReturn();

        /* Assert */

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void returnsNoStoreCredentialForValidSession() throws Exception {

        /* Arrange */

        var principal = principal(1);
        when(jwt.parseAccessToken("old"))
                .thenReturn(new JwtService.AccessToken(1L, 1L));
        when(users.loadUserById(1L)).thenReturn(principal);
        when(jwt.renewToken(principal, "old"))
                .thenReturn(new JwtService.RenewedToken("new", 600));

        /* Act */

        var result = mvc.perform(post("/api/auth/renew")
                        .header("Authorization", "Bearer old"))
                .andReturn();

        /* Assert */

        var body = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString());
        assertThat(result.getResponse().getStatus())
                .isEqualTo(200);
        assertThat(result.getResponse().getHeader("Cache-Control"))
                .isEqualTo("no-store");
        assertThat(body.path("token").asText())
                .isEqualTo("new");
        assertThat(body.path("expiresIn").asInt())
                .isEqualTo(600);
    }

    @Test
    void refusesRevokedVersionBeforeRenewal() throws Exception {

        /* Arrange */

        when(jwt.parseAccessToken("old"))
                .thenReturn(new JwtService.AccessToken(1L, 1L));
        when(users.loadUserById(1L)).thenReturn(principal(2));

        /* Act */

        var result = mvc.perform(post("/api/auth/renew")
                        .header("Authorization", "Bearer old"))
                .andReturn();

        /* Assert */

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void refusesTokenThatExpiresBeforeRenewal() throws Exception {

        /* Arrange */

        var principal = principal(1);
        when(jwt.parseAccessToken("old"))
                .thenReturn(new JwtService.AccessToken(1L, 1L));
        when(users.loadUserById(1L)).thenReturn(principal);
        when(jwt.renewToken(principal, "old"))
                .thenThrow(new JwtException("expired"));

        /* Act */

        var result = mvc.perform(post("/api/auth/renew")
                        .header("Authorization", "Bearer old"))
                .andReturn();

        /* Assert */

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }
}
