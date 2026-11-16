package pe.sotomayor.creditos.customers.domain.repository;

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
class CustomerCeUniquenessTest {
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    CustomerRepository repository;

    private void insert(String type, String number, boolean deleted) {
        jdbc.update("""
                INSERT INTO customers (customer_code, full_name, document_type, document_number, branch_id, is_deleted, deleted_at)
                VALUES (gen_random_uuid(), 'CLIENTE', ?, ?, (SELECT id FROM branches WHERE code='CENT'),
                        ?, CASE WHEN ? THEN now() ELSE NULL END)
                """, type, number, deleted, deleted);
    }

    @Test
    void findsOldLowercaseCeEvenIfDeleted() {

        /* Arrange */

        insert("CE", "ab000123", true);

        /* Act */

        boolean ce = repository.existsDocumentIncludingDeleted("CE", "AB000123");
        boolean dni = repository.existsDocumentIncludingDeleted("DNI", "AB000123");

        /* Assert */

        assertThat(ce).isTrue();
        assertThat(dni).isFalse();
    }

    @Test
    void databaseRejectsCaseVariantsEvenWhenPrecheckIsBypassed() {

        /* Arrange */

        insert("CE", "ab001", false);

        /* Act */

        var failure = catchThrowable(() -> insert("CE", "AB001", false));

        /* Assert */

        assertThat(failure).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sameTextInAnotherDocumentTypeRemainsIndependent() {

        /* Arrange */

        insert("CE", "12345678", false);
        insert("DNI", "12345678", false);

        /* Act */

        boolean ce = repository.existsDocumentIncludingDeleted("CE", "12345678");
        boolean dni = repository.existsDocumentIncludingDeleted("DNI", "12345678");

        /* Assert */

        assertThat(ce).isTrue();
        assertThat(dni).isTrue();
    }
}
