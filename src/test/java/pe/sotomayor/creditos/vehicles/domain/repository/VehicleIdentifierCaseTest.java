package pe.sotomayor.creditos.vehicles.domain.repository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import pe.sotomayor.creditos.support.PostgresTestContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresTestContainer.class)
class VehicleIdentifierCaseTest {
    @Autowired
    VehicleRepository vehicles;
    @Autowired
    JdbcTemplate jdbc;

    private Long insert(String chassis, String engine, boolean deleted) {
        return jdbc.queryForObject("""
                INSERT INTO vehicles (vehicle_code, vehicle_type, brand, model, manufacturing_year,
                  color, chassis_number, engine_number, reference_price, branch_id, is_deleted, deleted_at)
                VALUES (gen_random_uuid(), 'MOTO', 'MARCA', 'MODELO', 2026, 'ROJO', ?, ?, 5000,
                  (SELECT id FROM branches WHERE code = 'CENT'), ?, CASE WHEN ? THEN now() ELSE NULL END)
                RETURNING id
                """, Long.class, chassis, engine, deleted, deleted);
    }

    @Test
    void detectsCaseVariantsIncludingDeletedAndExcludesCurrentId() {

        /* Arrange */

        Long id = insert("chasis-prueba-uno", "motor-prueba-uno", true);

        /* Act */

        boolean chassis = vehicles.existsIdentifiersIncludingDeleted(
                "CHASIS-PRUEBA-UNO", "otro", 0L);
        boolean engine = vehicles.existsIdentifiersIncludingDeleted(
                "otro", "MOTOR-PRUEBA-UNO", 0L);
        boolean self = vehicles.existsIdentifiersIncludingDeleted(
                "CHASIS-PRUEBA-UNO", "MOTOR-PRUEBA-UNO", id);

        /* Assert */

        assertThat(chassis).isTrue();
        assertThat(engine).isTrue();
        assertThat(self).isFalse();
    }

    @Test
    void databasePreventsConcurrentStyleCaseVariantForChassis() {

        /* Arrange */

        insert("chasis-prueba-dos", "motor-prueba-dos", false);

        /* Act */

        var failure = catchThrowable(
                () -> insert("CHASIS-PRUEBA-DOS", "motor-diferente", false));

        /* Assert */

        assertThat(failure).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databasePreventsCaseVariantForEngine() {

        /* Arrange */

        insert("chasis-prueba-tres", "motor-prueba-tres", false);

        /* Act */

        var failure = catchThrowable(
                () -> insert("chasis-diferente", "MOTOR-PRUEBA-TRES", false));

        /* Assert */
        assertThat(failure).isInstanceOf(DataIntegrityViolationException.class);
    }
}
