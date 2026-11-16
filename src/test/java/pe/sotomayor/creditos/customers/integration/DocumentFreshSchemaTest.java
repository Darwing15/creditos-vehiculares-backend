package pe.sotomayor.creditos.customers.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import org.junit.jupiter.api.Test;

import org.testcontainers.containers.PostgreSQLContainer;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@Testcontainers
class DocumentFreshSchemaTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    private FluentConfiguration configure() {
        return Flyway.configure()
                .locations("classpath:db/migration", "classpath:db/test-migration")
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    @Test
    void freshSchemaEnforcesCaseInsensitiveIdentifiersAndKeepsStoredValues() throws Exception {

        /* Arrange */

        var flyway = configure().load();

        /* Act */

        var firstMigration = flyway.migrate();
        try (var connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword());
             var sql = connection.createStatement()) {
            sql.executeUpdate("""
                    INSERT INTO customers (customer_code, full_name, document_type, document_number, branch_id)
                    VALUES (gen_random_uuid(), 'CLIENTE', 'CE', 'ab001', (SELECT id FROM branches WHERE code='CENT'))
                    """);
            sql.executeUpdate("""
                    INSERT INTO vehicles (vehicle_code, vehicle_type, brand, model, manufacturing_year, color,
                        chassis_number, engine_number, reference_price, branch_id)
                    VALUES (gen_random_uuid(), 'MOTO', 'MARCA', 'MODELO', 2026, 'ROJO', 'chasis001', 'motor001', 5000,
                        (SELECT id FROM branches WHERE code='CENT'))
                    """);
        }
        // Una segunda ejecucion no simula actualizar un esquema V11 anterior.
        var secondMigration = configure().load().migrate();
        try (var connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword());
             var sql = connection.createStatement()) {
            String document, chassis;
            try (var rs = sql.executeQuery("SELECT document_number FROM customers WHERE document_type='CE'")) {
                if (!rs.next()) throw new IllegalStateException("No se encontró el cliente de prueba");
                document = rs.getString(1);
            }
            try (var rs = sql.executeQuery("SELECT chassis_number FROM vehicles")) {
                if (!rs.next()) throw new IllegalStateException("No se encontró el vehículo de prueba");
                chassis = rs.getString(1);
            }
            var duplicateDocument = catchThrowable(() -> sql.executeUpdate("""
                    INSERT INTO customers (customer_code, full_name, document_type, document_number, branch_id)
                    VALUES (gen_random_uuid(), 'OTRO', 'CE', 'AB001', (SELECT id FROM branches WHERE code='CENT'))
                    """));
            var duplicateChassis = catchThrowable(() -> sql.executeUpdate("""
                    INSERT INTO vehicles (vehicle_code, vehicle_type, brand, model, manufacturing_year, color,
                        chassis_number, engine_number, reference_price, branch_id)
                    VALUES (gen_random_uuid(), 'MOTO', 'MARCA', 'MODELO', 2026, 'ROJO', 'CHASIS001', 'motor002', 5000,
                        (SELECT id FROM branches WHERE code='CENT'))
                    """));
            var duplicateEngine = catchThrowable(() -> sql.executeUpdate("""
                    INSERT INTO vehicles (vehicle_code, vehicle_type, brand, model, manufacturing_year, color,
                        chassis_number, engine_number, reference_price, branch_id)
                    VALUES (gen_random_uuid(), 'MOTO', 'MARCA', 'MODELO', 2026, 'ROJO', 'CHASIS002', 'MOTOR001', 5000,
                        (SELECT id FROM branches WHERE code='CENT'))
                    """));

            /* Assert */

            assertThat(firstMigration.migrationsExecuted).isGreaterThanOrEqualTo(11);
            assertThat(secondMigration.migrationsExecuted).isZero();
            assertThat(document).isEqualTo("ab001");
            assertThat(chassis).isEqualTo("chasis001");
            assertThat(duplicateDocument).isInstanceOf(SQLException.class)
                    .hasMessageContaining("uk_customers_ce_document_upper");
            assertThat(duplicateChassis).isInstanceOf(SQLException.class)
                    .hasMessageContaining("uk_vehicles_chassis_number_upper");
            assertThat(duplicateEngine).isInstanceOf(SQLException.class)
                    .hasMessageContaining("uk_vehicles_engine_number_upper");
        }
    }
}
