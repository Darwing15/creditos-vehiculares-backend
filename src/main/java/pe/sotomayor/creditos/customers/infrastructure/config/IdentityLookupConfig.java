package pe.sotomayor.creditos.customers.infrastructure.config;

import java.net.http.HttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import pe.sotomayor.creditos.customers.domain.service.IdentityLookupCoordinator;
import pe.sotomayor.creditos.customers.domain.service.IdentityLookupProvider;
import pe.sotomayor.creditos.customers.infrastructure.identity.FactilizaIdentityLookupProvider;

/**
 * Transporte HTTPS exclusivo del proveedor; no reutiliza el JWT de la aplicacion.
 *
 * @author DarwinG
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IdentityLookupProperties.class)
public class IdentityLookupConfig {
    /**
     * @return adaptador con destino fijo y redirecciones deshabilitadas.
     */
    @Bean
    public IdentityLookupProvider identityLookupProvider(
            IdentityLookupProperties properties,
            ObjectMapper mapper) {
        properties.validate();
        var http = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER).build();

        var factory = new JdkClientHttpRequestFactory(http);

        factory.setReadTimeout(properties.getReadTimeout());

        var client = RestClient.builder().baseUrl("https://api.factiliza.com/v1")
                .requestFactory(factory).build();

        return new FactilizaIdentityLookupProvider(client, mapper, properties);
    }

    /**
     * @return coordinación de llamadas duplicadas dentro de esta instancia.
     */
    @Bean
    public IdentityLookupCoordinator identityLookupCoordinator() {
        return new IdentityLookupCoordinator();
    }
}

