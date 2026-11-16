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

Flyway administra la evolución del esquema; Hibernate utiliza ddl-auto=validate. En instalaciones nuevas, la V10 ya no crea cuentas ni sucursales. La nueva V10 tiene un checksum distinto: **no despliegues este JAR sobre una base que tenga aplicada la V10 anterior**. La documentación explica la transición con conservación de datos en una base separada.

## Inicialización y despliegue

Consultar [Inicialización y despliegue](docs/inicializacion-y-despliegue.md) para crear el primer administrador mediante consola o preparar una base nueva para restauración, sin contraseñas predefinidas. Los datos de prueba bajo `src/test` no se empaquetan en el JAR. Las bases anteriores requieren el procedimiento de transición documentado; no usar `flyway repair` como sustituto.

## Pruebas

```text
mvn test
```

Las pruebas de persistencia utilizan PostgreSQL aislado en Testcontainers y requieren Docker. Las consultas externas se simulan y no necesitan token real. Para regenerar MapStruct y validar solo el caso de uso de consulta:

```text
mvn -Dtest=LookupCustomerIdentityServiceTest clean test
```

El conteo de pruebas se registra en evidencia fechada de entrega y no constituye una meta fija. La inicialización del mapper en esa prueba debe utilizar Mappers.getMapper(IdentityLookupMapper.class).

## Listado paginado de clientes

Consulta [contrato de inventario, resumen y pruebas](docs/vehicles/inventario-listado-paginado.md). El catálogo previo continúa disponible.

## Inventario paginado

Consulta [contrato de inventario, resumen y pruebas](docs/vehicles/inventario-listado-paginado.md). El catálogo previo continúa disponible.

## Solicitudes paginadas

Consulta [contrato y permisos de la búsqueda de solicitudes](docs/credit/solicitudes-listado-paginado.md). La ruta anterior continúa disponible para sus consumidores.