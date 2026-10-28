# Sistema de Créditos Vehiculares

Backend Java 21 y Spring Boot 3.5 con PostgreSQL, Flyway, JPA y seguridad JWT.

## Documentación

- [Módulo Customers](src/main/java/pe/sotomayor/creditos/customers/README.md)
- [Consulta DNI con Factiliza](docs/customers/factiliza-dni.md)
- [Estándar de pruebas](docs/testing-standard.md)

La documentación funcional y los ADR se mantienen también en los documentos de proyecto. El anexo técnico versionado describe el contrato implementado, sus parámetros y la operación cotidiana.

## Configuración local

La conexión a PostgreSQL, la clave JWT y las credenciales externas se configuran fuera del código. El archivo creditos-local.properties utilizado por el entorno debe estar cargado por la configuración de arranque; application.yaml no declara una importación automática con ese nombre.

Para Factiliza, configurar app.identity-lookup.token sin el prefijo Bearer y app.identity-lookup.enabled=true. Conservar el token únicamente en el archivo privado del backend y reiniciar al cambiarlo. Los ejemplos del anexo utilizan valores sintéticos, no credenciales reales.

El límite mensual predeterminado de 100 expresa presupuesto local, no el saldo de la cuenta externa. Ajustarlo si existen consumos desde otros sistemas o el panel.

## Migraciones

Flyway administra la evolución del esquema; Hibernate utiliza ddl-auto=validate. La integración agregó V11__identity_document_lookup.sql, con caché de nombres y contadores técnicos. No editar una migración ya aplicada. Cualquier ampliación de comentarios del esquema se incorpora mediante una versión nueva revisada.

## Pruebas

```text
mvn test
```

Las pruebas de persistencia utilizan PostgreSQL aislado en Testcontainers y requieren Docker. Las consultas externas se simulan y no necesitan token real. Para regenerar MapStruct y validar solo el caso de uso de consulta:

```text
mvn -Dtest=LookupCustomerIdentityServiceTest clean test
```

El conteo de pruebas se registra en evidencia fechada de entrega y no constituye una meta fija. La inicialización del mapper en esa prueba debe utilizar Mappers.getMapper(IdentityLookupMapper.class).
