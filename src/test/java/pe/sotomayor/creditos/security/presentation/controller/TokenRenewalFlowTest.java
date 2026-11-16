package pe.sotomayor.creditos.security.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;

import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.sotomayor.creditos.security.infrastructure.security.config.SecurityConfig;
import pe.sotomayor.creditos.security.infrastructure.security.config.SecurityErrorHandler;
import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtProperties;
import pe.sotomayor.creditos.security.infrastructure.security.jwt.JwtService;
import pe.sotomayor.creditos.security.infrastructure.security.model.SecurityPrincipal;
import pe.sotomayor.creditos.security.infrastructure.security.service.CustomUserDetailsService;

import pe.sotomayor.creditos.shared.config.TimeConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(TokenRenewalController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class,
        JwtService.class, JwtProperties.class, TimeConfig.class})
@TestPropertySource(properties = {
        "app.security.allowed-origins=http://localhost:5173", "app.security.docs-enabled=false",
        "spring.security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "spring.security.jwt.issuer=creditos-api", "spring.security.jwt.audience=creditos-web"})
class TokenRenewalFlowTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwt;
    @Autowired
    ObjectMapper json;
    @MockitoBean
    CustomUserDetailsService users;

    private SecurityPrincipal principal(long version) {
        return new SecurityPrincipal(
                1L, version,
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
    void signedTokenPassesFilterRenewsAndStopsAfterRevocation() throws Exception {

        /* Arrange */

        String original = jwt.generateToken(principal(1));
        when(users.loadUserById(1L)).thenReturn(principal(1));

        /* Act */

        var result = mvc.perform(post("/api/auth/renew")
                        .header("Authorization", "Bearer " + original))
                .andReturn();
        String renewed = json.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
        var parsed = jwt.parseAccessToken(renewed);
        when(users.loadUserById(1L)).thenReturn(principal(2));
        var revoked = mvc.perform(post("/api/auth/renew")
                .header("Authorization", "Bearer " + renewed)).andReturn();

        /* Assert */

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(parsed.userId()).isEqualTo(1L);
        assertThat(revoked.getResponse().getStatus()).isEqualTo(401);
    }
}
