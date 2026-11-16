package pe.sotomayor.creditos.bootstrap;

import org.flywaydb.core.Flyway;

import java.io.Console;

import java.net.URI;
import java.net.URLDecoder;

import java.nio.charset.StandardCharsets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

/**
 * Herramienta explicita de consola: nunca se ejecuta al iniciar el servidor web.
 *
 * @author DarwinG
 */
public final class InitialAdminBootstrap {
    private InitialAdminBootstrap() {
    }

    public static void main(String[] args) {
        if (args.length != 0 || System.console() == null) {
            System.err.println("Ejecute la herramienta sin argumentos, desde una terminal interactiva.");
            System.exit(2);
        }
        try {
            run(System.console());
        } catch (Exception failure) {
            // No imprimir excepciones: podrían incluir datos de conexión o valores enviados al servidor.
            System.err.println("No se completó la operación. Compruebe la base, sus permisos y el historial Flyway.");
            System.err.println("Si hubo una interrupción, inspeccione el estado antes de volver a intentarlo.");
            System.exit(1);
        }
    }

    private static void run(Console console) throws Exception {
        String action = required(console, "Escriba INICIALIZAR para base nueva o PREPARAR para restauración: ");
        if (!action.equals("INICIALIZAR") && !action.equals("PREPARAR")) {
            throw new IllegalArgumentException("Operación no reconocida.");
        }
        String jdbcUrl = required(console, "URL JDBC PostgreSQL sin usuario ni contraseña: ");
        checkJdbcUrl(jdbcUrl);
        String dbUser = required(console, "Usuario de PostgreSQL: ");
        char[] dbPassword = console.readPassword("Contraseña de PostgreSQL: ");
        if (dbPassword == null) throw new IllegalArgumentException("Entrada cancelada.");
        char[] password = null;
        char[] confirmation = null;
        try {
            String dbSecret = new String(dbPassword);
            Properties properties = new Properties();
            properties.setProperty("user", dbUser);
            properties.setProperty("password", dbSecret);
            properties.setProperty("ApplicationName", "creditos-inicializacion");
            try (Connection connection = DriverManager.getConnection(jdbcUrl, properties)) {
                String database;
                String server;
                try (Statement sql = connection.createStatement();
                     ResultSet row = sql.executeQuery("SELECT current_database(), coalesce(inet_server_addr()::text, 'local'), inet_server_port()")) {
                    if (!row.next()) throw new SQLException("No se pudo identificar el destino.");
                    database = row.getString(1);
                    server = row.getString(2) + ":" + row.getString(3);
                }
                console.printf("Destino: base %s, servidor %s. Solo se admite una base nueva sin datos de aplicación.%n", database, server);
                if (!required(console, "Escriba exactamente el nombre de la base para confirmar: ").equals(database)) {
                    throw new IllegalArgumentException("Confirmación incorrecta.");
                }
                Flyway flyway = Flyway.configure().dataSource(jdbcUrl, dbUser, dbSecret)
                        .schemas("public").defaultSchema("public")
                        .locations("classpath:db/migration").load();
                if (isEmptyDatabase(connection)) {
                    flyway.migrate();
                } else if (action.equals("INICIALIZAR")) {
                    // Permite retomar después de preparar el esquema o cancelar la captura,
                    // pero nunca migrar ni sembrar encima de datos existentes.
                    flyway.validate();
                    if (flyway.info().pending().length != 0)
                        throw new IllegalStateException("Hay migraciones pendientes.");
                    requireEmptyApplicationData(connection);
                } else {
                    throw new IllegalStateException("La base no está vacía; no se alterará.");
                }

                if (action.equals("PREPARAR")) {
                    console.printf("Esquema preparado; no se creó ningún usuario ni sucursal.%n");
                    return;
                }
                String branchCode = required(console, "Código de la sucursal inicial: ");
                String branchName = required(console, "Nombre de la sucursal inicial: ");
                String branchAddress = required(console, "Dirección de la sucursal inicial: ");
                String fullName = required(console, "Nombre del administrador: ");
                String email = required(console, "Correo del administrador: ");
                password = console.readPassword("Contraseña nueva del administrador (15-72 caracteres, máximo 72 bytes UTF-8): ");
                confirmation = console.readPassword("Repita la contraseña nueva: ");
                if (password == null || confirmation == null || !Arrays.equals(password, confirmation)) {
                    throw new IllegalArgumentException("Las contraseñas no coinciden o se canceló la entrada.");
                }
                InitialAdminProvisioner.create(connection, branchCode, branchName, branchAddress, fullName, email, password);
                console.printf("Administrador inicial y sucursal creados. Inicie sesión con la contraseña elegida.%n");
            }
        } finally {
            Arrays.fill(dbPassword, '\0');
            if (password != null) Arrays.fill(password, '\0');
            if (confirmation != null) Arrays.fill(confirmation, '\0');
        }
    }

    private static String required(Console console, String prompt) {
        String value = console.readLine(prompt);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Entrada vacía o cancelada.");
        return value.strip();
    }

    static void checkJdbcUrl(String value) {
        if (!value.startsWith("jdbc:postgresql://")) throw new IllegalArgumentException("URL JDBC inválida.");
        URI uri = URI.create(value.substring("jdbc:".length()));
        if (uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("La URL no debe contener credenciales.");
        }
        if (uri.getRawQuery() != null) {
            for (String parameter : uri.getRawQuery().split("&")) {
                String name = URLDecoder.decode(parameter.split("=", 2)[0], StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                if (name.contains("password") || name.equals("user") || name.contains("secret")) {
                    throw new IllegalArgumentException("La URL no debe contener credenciales.");
                }
            }
        }
    }

    private static boolean isEmptyDatabase(Connection connection) throws SQLException {
        try (Statement sql = connection.createStatement(); ResultSet row = sql.executeQuery("""
                SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname !~ '^pg_' AND n.nspname <> 'information_schema'
                  AND c.relkind IN ('r', 'p', 'v', 'm', 'S', 'f')
                """)) {
            if (!row.next()) throw new SQLException("No se pudo verificar la base.");
            return row.getLong(1) == 0;
        }
    }

    private static void requireEmptyApplicationData(Connection connection) throws SQLException {
        try (Statement names = connection.createStatement(); ResultSet tables = names.executeQuery("""
                SELECT n.nspname, c.relname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname !~ '^pg_' AND n.nspname <> 'information_schema'
                  AND c.relkind IN ('r', 'p')
                  AND NOT (n.nspname = 'public' AND c.relname IN ('flyway_schema_history', 'security_admin_guard'))
                """)) {
            while (tables.next()) {
                try (Statement check = connection.createStatement()) {
                    String table = check.enquoteIdentifier(tables.getString(1), true) + "."
                            + check.enquoteIdentifier(tables.getString(2), true);
                    try (ResultSet row = check.executeQuery("SELECT EXISTS (SELECT 1 FROM " + table + ")")) {
                        if (!row.next() || row.getBoolean(1))
                            throw new IllegalStateException("La base contiene datos.");
                    }
                }
            }
        }
    }
}
