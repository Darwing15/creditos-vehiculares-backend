package pe.sotomayor.creditos;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;

import pe.sotomayor.creditos.support.PostgresTestContainer;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * Verifica el arranque con PostgreSQL aislado de Testcontainers.
 * Importar la configuracion evita depender de DB_URL de desarrollo.
 * El secreto de esta prueba es ficticio y no debe utilizarse en producción.
 *
 * @author DarwinG
 */
@SpringBootTest(properties = {
        "spring.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.security.allowed-origins=http://localhost:5173",
        "app.security.docs-enabled=false"
})
@Import(PostgresTestContainer.class)
class CreditosSystemApplicationTests {

    @Autowired
    ConfigurableApplicationContext context;

    @Test
    void contextLoads() {
        /* Arrange */
        var applicationContext = context;

        /* Act */
        boolean active = applicationContext.isActive();

        /* Assert */
        assertThat(active).isTrue();
    }

}
