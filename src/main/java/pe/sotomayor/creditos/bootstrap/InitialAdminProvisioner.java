package pe.sotomayor.creditos.bootstrap;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import pe.sotomayor.creditos.security.application.dto.branch.request.CreateBranchRequest;
import pe.sotomayor.creditos.security.application.dto.user.request.CreateUserRequest;
import pe.sotomayor.creditos.security.domain.model.enumeration.SecurityPermission;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Locale;

/**
 * Crea la primera identidad administrativa en una base nueva y vacia.
 *
 * @author DarwinG
 */
public final class InitialAdminProvisioner {

    private InitialAdminProvisioner() {
    }

    public static void create(Connection connection, String branchCode, String branchName,
                              String branchAddress, String fullName, String email,
                              char[] password) throws SQLException {
        if (connection == null || branchCode == null || branchName == null || branchAddress == null
                || fullName == null || email == null || password == null) {
            throw new IllegalArgumentException("Faltan datos obligatorios.");
        }
        String code = branchCode.strip().toUpperCase(Locale.ROOT);
        String name = branchName.strip();
        String address = branchAddress.strip();
        String adminName = fullName.strip();
        String adminEmail = email.strip().toLowerCase(Locale.ROOT);
        String secret = new String(password);

        // Los DTO aplican la misma validación que el alta habitual, incluido el límite BCrypt en bytes.
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            if (!validator.validate(new CreateBranchRequest(code, name, address, null, null, null)).isEmpty()
                    || !validator.validate(new CreateUserRequest(adminName, adminEmail, secret, 1L, 1L)).isEmpty()) {
                throw new IllegalArgumentException("Datos inválidos: revise los campos y la política de contraseña.");
            }
        }
        if (!connection.getAutoCommit()) {
            throw new IllegalStateException("Se requiere una conexión sin transacción previa.");
        }

        // El texto claro nunca se envía a PostgreSQL; solo llega el hash BCrypt.
        String hash = new BCryptPasswordEncoder(12).encode(secret);
        connection.setAutoCommit(false);
        boolean transactionFinished = false;
        try {
            try (Statement sql = connection.createStatement();
                 ResultSet row = sql.executeQuery("SELECT singleton FROM public.security_admin_guard WHERE singleton = 1 FOR UPDATE")) {
                if (!row.next()) throw new IllegalStateException("No existe la protección de administración.");
            }
            try (Statement sql = connection.createStatement()) {
                sql.execute("LOCK TABLE public.roles, public.branches, public.users, public.role_permissions IN SHARE ROW EXCLUSIVE MODE");
                try (ResultSet row = sql.executeQuery("""
                        SELECT (SELECT count(*) FROM public.roles) + (SELECT count(*) FROM public.branches)
                          + (SELECT count(*) FROM public.users) + (SELECT count(*) FROM public.role_permissions)
                        """)) {
                    if (!row.next() || row.getLong(1) != 0) {
                        throw new IllegalStateException("La base ya contiene datos de seguridad; no se inicializa.");
                    }
                }
            }
            long role = insertId(connection,
                    "INSERT INTO public.roles (name, description) VALUES (?, ?) RETURNING id",
                    "ADMIN", "Administración inicial");
            long branch = insertId(connection,
                    "INSERT INTO public.branches (code, name, address, is_active) VALUES (?, ?, ?, TRUE) RETURNING id",
                    code, name, address);
            try (PreparedStatement sql = connection.prepareStatement("INSERT INTO public.role_permissions (role_id, permission) VALUES (?, ?)")) {
                for (String authority : SecurityPermission.authorities()) {
                    sql.setLong(1, role);
                    sql.setString(2, authority);
                    sql.addBatch();
                }
                sql.executeBatch();
            }
            try (PreparedStatement sql = connection.prepareStatement("""
                    INSERT INTO public.users (full_name, email, password, role_id, branch_id, is_active)
                    VALUES (?, ?, ?, ?, ?, TRUE)
                    """)) {
                sql.setString(1, adminName);
                sql.setString(2, adminEmail);
                sql.setString(3, hash);
                sql.setLong(4, role);
                sql.setLong(5, branch);
                if (sql.executeUpdate() != 1) throw new SQLException("No se creó el administrador.");
            }
            connection.commit();
            transactionFinished = true;
        } catch (SQLException | RuntimeException failure) {
            try {
                connection.rollback();
                transactionFinished = true;
            } catch (SQLException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        } finally {
            // Si no se pudo hacer rollback, no habilitar autocommit: podría confirmar trabajo parcial.
            if (transactionFinished) connection.setAutoCommit(true);
        }
    }

    private static long insertId(Connection connection, String sql, String... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setString(i + 1, values[i]);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new SQLException("No se pudo crear el dato inicial.");
                return row.getLong(1);
            }
        }
    }
}
