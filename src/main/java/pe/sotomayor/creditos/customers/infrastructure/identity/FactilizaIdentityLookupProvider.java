package pe.sotomayor.creditos.customers.infrastructure.identity;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.net.SocketTimeoutException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;

import pe.sotomayor.creditos.customers.domain.exception.IdentityLookupException;
import pe.sotomayor.creditos.customers.domain.model.valueobject.IdentityData;
import pe.sotomayor.creditos.customers.domain.service.IdentityLookupProvider;

import pe.sotomayor.creditos.customers.infrastructure.config.IdentityLookupProperties;

/**
 * Traduce Factiliza a datos minimos; descarta cuerpos y causas que puedan revelar datos personales.
 *
 * @author DarwinG
 */
@RequiredArgsConstructor
public class FactilizaIdentityLookupProvider implements IdentityLookupProvider {

    private final RestClient client;
    private final ObjectMapper mapper;
    private final IdentityLookupProperties properties;

    /**
     * {@inheritDoc}
     */
    @Override
    public IdentityData lookup(String documentNumber) {
        if (documentNumber == null
                || !documentNumber.matches("[0-9]{8}"))
            throw IdentityLookupException.invalidResponse();
        if (!properties.enabled())
            throw IdentityLookupException.disabled();
        try {
            return client.get().uri("/dni/info/{dni}", documentNumber)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(properties.getToken()))
                    .exchange((request, response) -> {
                        checkStatus(response.getStatusCode().value());

                        byte[] body = response.getBody().readNBytes(65_537);

                        if (body.length > 65_536)
                            throw IdentityLookupException.invalidResponse();

                        return parse(body, documentNumber);
                    });
        } catch (IdentityLookupException controlled) {
            throw controlled;
        } catch (ResourceAccessException failure) {
            for (Throwable cause = failure; cause != null; cause = cause.getCause())
                if (cause instanceof HttpTimeoutException
                        || cause instanceof SocketTimeoutException)
                    throw IdentityLookupException.timeout();
            throw IdentityLookupException.unavailable();
        } catch (RestClientException failure) {
            throw IdentityLookupException.unavailable();
        }
    }

    private IdentityData parse(byte[] body, String requested) {
        try {
            JsonNode root = mapper.readTree(body);

            if (root == null || !root.isObject())
                throw IdentityLookupException.invalidResponse();

            int status = root.path("status").asInt(-1);

            if (status != 200) checkStatus(status);

            if (!root.path("success").isBoolean()
                    || !root.path("success").booleanValue())
                throw IdentityLookupException.unavailable();

            JsonNode data = root.path("data");

            String number = text(data, "numero");

            if (!requested.equals(number))
                throw IdentityLookupException.invalidResponse();

            return new IdentityData(
                    number,
                    text(data, "nombres"),
                    text(data, "apellido_paterno"),
                    text(data, "apellido_materno"),
                    "FACTILIZA"
            );
        } catch (IOException failure) {
            throw IdentityLookupException.invalidResponse();
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull())
            return null;
        if (!value.isTextual())
            throw IdentityLookupException.invalidResponse();

        return value.textValue();
    }

    private void checkStatus(int status) {
        if (status == 200)
            return;
        if (status == 404)
            throw IdentityLookupException.notFound();
        if (status == 429)
            throw IdentityLookupException.rateLimited();
        if (status == 408 || status == 504)
            throw IdentityLookupException.timeout();
        /* El 400 documentado es generico: no demuestra que un DNI no exista ni que se agoto saldo. */
        if (status == 400 || status == 401 || status == 402 || status == 403 || status >= 500)
            throw IdentityLookupException.unavailable();

        throw IdentityLookupException.invalidResponse();
    }
}
