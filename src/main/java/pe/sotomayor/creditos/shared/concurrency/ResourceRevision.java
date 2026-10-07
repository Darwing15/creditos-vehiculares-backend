package pe.sotomayor.creditos.shared.concurrency;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.HexFormat;
import java.util.Objects;

import pe.sotomayor.creditos.shared.error.ErrorCode;
import pe.sotomayor.creditos.shared.exception.ConflictException;

/**
 * Revision del contenido editable; se compara despues de bloquear la fila.
 *
 * @author DarwinG
 */
public final class ResourceRevision {
    private ResourceRevision() {
    }

    /**
     * Calcula una huella con valores prefijados por longitud y decimales canonicos.
     */
    public static String of(Object... fields) {
        StringBuilder content = new StringBuilder();
        for (Object field : fields) {
            if (field == null) {
                content.append("-1:");
                continue;
            }
            String value = field instanceof BigDecimal amount
                    ? amount.stripTrailingZeros().toPlainString() : field.toString();
            content.append(value.length()).append(':').append(value);
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /**
     * Rechaza la actualizacion si el contenido actual difiere del detalle revisado.
     */
    public static void requireMatch(String expected, String current) {
        if (!Objects.equals(expected, current)) {
            throw new ConflictException(ErrorCode.CONCURRENT_MODIFICATION, "Reload the record before updating it");
        }
    }
}
