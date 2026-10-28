# Testing Standard - Sistema de Créditos

## 1. Objetivo
Establecer un estándar único para el desarrollo de pruebas automatizadas del Sistema de Créditos, garantizando:
* Uniformidad entre módulos.
* Alta cobertura de reglas de negocio.
* Bajo acoplamiento entre pruebas.
* Facilidad de mantenimiento.
* Detección temprana de regresiones.

## 2. Pirámide de pruebas
Seguiremos la pirámide clásica.

```text
                E2E
                 ▲
          Integration Tests
                 ▲
            Unit Tests
```

Distribución aproximada:

| Tipo | Porcentaje |
| :--- | :--- |
| Unit Tests | 70% |
| Integration Tests | 25% |
| End-to-End | 5% |

La mayor parte de la lógica deberá estar protegida mediante pruebas unitarias.

## 3. Estructura del proyecto
```text
src
 ├── main
 └── test
      └── java
          └── pe.sotomayor.creditos
              ├── config
              └── security
                  ├── application
                  ├── infrastructure
                  └── presentation
```
La estructura de test debe reflejar la de `main`.

Ejemplo:  
**main**  
`application/service/auth/AuthenticationService`  
`↓`  
**test**  
`application/service/auth/AuthenticationServiceTest`

Esto facilita encontrar rápidamente la prueba correspondiente a cada clase.

## 4. Convención de nombres
Cada clase tendrá una única clase de prueba.

Ejemplo:  
`AuthenticationService`  
`↓`  
`AuthenticationServiceTest`

Los métodos seguirán el patrón:

`should<Result>When<Condition>()`

Ejemplos:
* `shouldAuthenticateUserWhenCredentialsAreValid()`
* `shouldThrowUnauthorizedExceptionWhenPasswordIsIncorrect()`
* `shouldChangePasswordSuccessfully()`
* `shouldIncreaseTokenVersionAfterPasswordChange()`

Este formato hace que el nombre del test sea prácticamente documentación.

## 5. Organización interna (AAA)
Cada prueba seguirá estrictamente el patrón:
1. **Arrange** (Preparación)
2. **Act** (Acción)
3. **Assert** (Validación)

Ejemplo:
```java
@Test
void shouldAuthenticateUserWhenCredentialsAreValid() {
    /* Arrange */
    ...
    /* Act */
    ...
    /* Assert */
    ...
}
```
No mezclaremos preparación con validaciones.

## 6. Herramientas
Utilizaremos únicamente:
* JUnit 5
* Mockito
* AssertJ (estándar para todas las aserciones)
* Spring Boot Test
* MockMvc

Todas las aserciones deberán realizarse mediante AssertJ.  
No se utilizarán las aserciones de JUnit (`assertEquals`, `assertTrue`, `assertThrows`, etc.), con el fin de mantener un estilo uniforme y aprovechar la mayor expresividad de AssertJ.

No incorporaremos librerías adicionales salvo necesidad justificada.

## 7. ¿Qué se prueba?

### Servicios
**Siempre.**  
Todo caso de uso tendrá pruebas unitarias.

Ejemplo:  
`AuthenticationService`
* ✓ login correcto
* ✓ usuario inexistente
* ✓ contraseña incorrecta
* ✓ usuario inactivo
* ✓ JWT generado

### Controladores/Serialización
**No se prueban mediante mocks.**  
Se prueban mediante:  
`MockMvc`

Porque el objetivo es validar:
* endpoints
* JSON
* status HTTP
* validaciones
* serialización

No la lógica.

Al ampliar un DTO de listado, la regresión deberá comprobar el JSON que usa la interfaz con el mapper real. En clientes, customerCode y address; en vehículos, chassisNumber y engineNumber. Una prueba de navegador con un mock que ya incluya esos campos no demuestra que el backend los devuelve.

### Repositorios
**No tendrán pruebas unitarias.**  
Únicamente pruebas de integración cuando exista:
* JPQL personalizada
* JOIN FETCH
* consultas complejas

Ejemplo:  
`findUserWithRoleAndBranchByEmail()`  
Sí merece una prueba.

## 8. Uso de Mockito

### `@Mock`
Para todas las dependencias externas.  
Ejemplo:
```java
@Mock
private UserRepository repository;
```

### `@InjectMocks`
Siempre sobre el caso de uso.
```java
@InjectMocks
private AuthenticationService service;
```

### Nunca
No se mockean:
* DTOs
* Records
* Objetos simples

Se instancian normalmente.

## 8.1 Estándar de Assertions

Todas las validaciones deberán realizarse utilizando **AssertJ**.

No se utilizarán las clases de assertions de JUnit (`Assertions.assertEquals`, `assertTrue`, `assertThrows`, etc.), salvo que exista una necesidad técnica específica.

Ejemplos:

```java
assertThat(response.userId())
        .isEqualTo(1L);

assertThat(response.token())
        .isNotBlank();

assertThatThrownBy(() -> service.authenticate(request))
        .isInstanceOf(ResourceNotFoundException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_NOT_FOUND);
```

Esto proporciona:

* API fluida.
* Mejor legibilidad.
* Mensajes de error más descriptivos.
* Un único estándar para todo el proyecto.

## 8.2 Organización de las verificaciones

Cuando una prueba utilice Mockito, las verificaciones (`verify`) deberán ubicarse dentro de la sección Assert, inmediatamente después de las aserciones funcionales (`assertThat`).

Orden recomendado:

```java
/* Arrange */

...

/* Act */

...

/* Assert */

assertThat(...);

verify(...);

verifyNoMoreInteractions(...);
```

Las verificaciones forman parte de la validación del comportamiento esperado del componente bajo prueba y, por tanto, deben considerarse parte de la fase Assert del patrón AAA.

## 8.3 Colaboradores deterministas en pruebas de servicio

Las dependencias externas se simulan mediante Mockito. Cuando la prueba del caso de uso utiliza una conversión determinista, puede inyectarse la implementación real del mapper. Si se mantiene @InjectMocks y se necesita registrarla como spy, el campo debe inicializarse con Mappers.getMapper(...).

```java
@Spy
IdentityLookupMapper mapper =
        Mappers.getMapper(IdentityLookupMapper.class);
```

Importar org.mapstruct.factory.Mappers. Un @Spy de una interfaz sin inicialización no selecciona su implementación generada y un método abstracto puede devolver null. No se simulan toEntry ni toResponse para hacer pasar un escenario exitoso. Este patrón aplica a colaboradores reales en pruebas del servicio; las pruebas cuya unidad es el mapper conservan la regla de no utilizar Mockito.

## 9. Qué NO probar
No probar:
* getters
* setters
* records
* builders
* Lombok
* JPA
* Spring Framework

Solo se prueba código propio.

## 10. Cobertura esperada

| Capa | Cobertura objetivo |
| :--- | :--- |
| Application | `>`95% |
| Infrastructure | `>`80% |
| Presentation | `>`80% |
| Domain | 100% cuando exista lógica |

No buscamos un 100% artificial, sino cubrir todas las reglas de negocio.

## 11. Datos de prueba
Nunca reutilizar datos entre pruebas.  
Cada prueba debe construir únicamente la información que necesita.

Ejemplo:
```java
User user = User.builder()
        .id(1L)
        .email("admin@inversiones.com")
        .password(encodedPassword)
        .active(true)
        .build();
```
Cada prueba debe ser completamente independiente.

## 12. Una sola responsabilidad por prueba

**Incorrecto:**
```java
@Test
void login() {
    login();
    changePassword();
    me();
}
```

**Correcto:**
* ✓ login
* ✓ changePassword
* ✓ me

Un test = una regla de negocio.

## 13. Probar únicamente la responsabilidad de la clase

Cada prueba debe validar exclusivamente el comportamiento propio de la clase bajo prueba.

Cuando una responsabilidad haya sido delegada a otro componente (por ejemplo, `AuthenticationManager`, `JwtService`, `PasswordEncoder` o cualquier servicio externo), la prueba no deberá volver a validar dicha lógica. En estos casos únicamente se verificará la interacción con la dependencia mediante mocks.

Ejemplo:

* `AuthenticationService` no valida contraseñas; esa responsabilidad pertenece a Spring Security.
* `AuthenticationService` únicamente debe comprobar que:
    * delega la autenticación al `AuthenticationManager`;
    * recupera correctamente el usuario;
    * solicita la generación del JWT;
    * construye la respuesta esperada.

Este principio evita pruebas duplicadas, reduce el acoplamiento entre componentes y mantiene alineadas las pruebas con la arquitectura del sistema.

## 14. Pruebas de integración
Utilizar:
```java
@SpringBootTest
@AutoConfigureMockMvc
```
Solo cuando intervengan varias capas.  
Por ejemplo:  
`Controller`   
`↓`  
`Security`  
`↓`  
`Service`  
`↓`  
`Repository`

## 15. End-to-End
Solo para flujos completos.  
Ejemplo:  
`Login`  
`↓`  
`/me`  
`↓`  
`change password`  
`↓`  
`JWT anterior`  
`↓`  
`401`  
`↓`  
`login`  
`↓`  
`JWT nuevo`  
`↓`  
`/me`  
`↓`  
`200`

No deberían superar el 5% del total de pruebas.

## 16. Javadoc
Las clases de prueba no llevarán Javadoc.  
Los métodos de prueba tampoco.

Los nombres de los métodos ya describen claramente el comportamiento esperado, evitando documentación redundante.

## 17. Principios arquitectónicos
Toda prueba deberá cumplir:
* Independiente de otras pruebas.
* Repetible.
* Determinística.
* Rápida.
* Fácil de leer.
* Fácil de mantener.

## 18. Construcción de datos de prueba
Los objetos utilizados durante la preparación de una prueba deberán construirse mediante métodos privados auxiliares ubicados al final de la clase de prueba.

Ejemplo:
```java
@Test
void shouldAuthenticateUserWhenCredentialsAreValid() {
    /* Arrange */
    LoginRequest request = createRequest();
    User user = createUser();
    /* Act */
    ...
    /* Assert */
    ...
}
```
Los métodos auxiliares encapsularán la creación de entidades, DTOs y objetos de dominio, evitando duplicación y mejorando la legibilidad de las pruebas.

**Reglas:**
* Cada método debe construir un único tipo de objeto (`createUser()`, `createRole()`, `createBranch()`, etc.).
* Los métodos se ubicarán al final de la clase de prueba.
* Los datos retornados deben representar un escenario válido por defecto. Las pruebas modificarán únicamente los atributos necesarios para el caso específico.
* No se reutilizarán métodos entre clases de prueba. Cuando exista una necesidad recurrente en múltiples módulos, la extracción a una fábrica de datos de prueba (Test Data Factory) se evaluará como una mejora arquitectónica futura.

## 18.1 Construcción de datos válidos por defecto

Todos los métodos auxiliares (`createUser()`, `createRequest()`, `createRole()`, etc.) deberán construir objetos completamente válidos.

Ejemplo:

```java
User user = createUser();
```

representa siempre un usuario válido.

Las pruebas modificarán únicamente el atributo necesario para representar el escenario que desean validar.

Ejemplo:

```java
User user = createUser();
user.setActive(false);
```

o

```java
when(passwordEncoder.matches(...))
        .thenReturn(false);
```

Nunca se crearán métodos como:

* createInactiveUser()
* createInvalidUser()
* createWrongPasswordUser()

porque generan duplicación y aumentan el costo de mantenimiento.

Existe un único escenario válido por defecto y cada prueba adapta únicamente aquello que necesita.

**Extensión para Value Objects**

Los métodos auxiliares también podrán utilizarse para construir Value Objects o valores del dominio (por ejemplo, `BigDecimal`, `LocalDate`, `UUID`, etc.) cuando estos representen información significativa y sean reutilizados por varias pruebas.

Ejemplos:

```java
private BigDecimal createVehiclePrice() {
    return new BigDecimal("50000.00");
}

private LocalDate createStartDate() {
    return LocalDate.of(2026, 7, 1);
}
```

Esto evita la repetición de valores literales, mejora la legibilidad y mantiene el mismo criterio aplicado a entidades y DTOs: partir siempre de datos válidos por defecto y modificar únicamente aquello que cada prueba necesita.

## 18.2 Verificación de interacciones

Además de validar el resultado esperado, las pruebas unitarias deberán verificar las interacciones con las dependencias externas utilizando Mockito.

Reglas:

* Verificar únicamente las interacciones que forman parte de la responsabilidad del caso de uso.
* Utilizar `verifyNoInteractions(...)` cuando una dependencia no deba ejecutarse.
* Finalizar cada prueba con `verifyNoMoreInteractions(...)` para detectar llamadas inesperadas.

Ejemplo:

```java
verify(userRepository)
        .save(user);

verify(jwtService)
        .generateToken(user);

verifyNoMoreInteractions(
        userRepository,
        jwtService);
```

## 18.3 Construcción de Authentication

Cuando un caso de uso dependa del `SecurityContextHolder`, las pruebas deberán construir una autenticación válida mediante el constructor autenticado de `UsernamePasswordAuthenticationToken`.

Ejemplo:

```java
private Authentication createAuthentication() {

    return new UsernamePasswordAuthenticationToken(
            "admin@inversiones.com",
            null,
            Collections.emptyList()
    );
}
```
El uso del constructor con autoridades evita que Spring Security considere la autenticación como no autenticada durante la ejecución de la prueba.

## 18.4 Limpieza del estado global

Algunas clases, especialmente las relacionadas con seguridad, utilizan componentes que mantienen estado global durante la ejecución, como `SecurityContextHolder`.

Toda prueba que modifique este tipo de estado deberá restaurarlo al finalizar para garantizar el aislamiento entre pruebas.

Ejemplo:

```java
@AfterEach
void tearDown() {

    SecurityContextHolder.clearContext();
}
```

Reglas:

* Ninguna prueba debe depender del estado dejado por otra.
* El estado global deberá inicializarse explícitamente durante el Arrange cuando sea necesario.
* El estado global deberá limpiarse al finalizar cada prueba.
* Esta regla aplica especialmente a filtros, componentes de seguridad y cualquier clase que utilice ThreadLocal o mecanismos similares.

## 18.5 Verificación del comportamiento antes que de la implementación

Las pruebas unitarias deberán priorizar la validación del comportamiento observable de la unidad bajo prueba sobre la verificación de su implementación interna.

Cuando el resultado esperado demuestre que una colaboración interna ocurrió correctamente, no será necesario verificar explícitamente cada invocación mediante Mockito.

Ejemplo:

```java
Authentication authentication =
        SecurityContextHolder
                .getContext()
                .getAuthentication();

assertThat(authentication)
        .isNotNull();

assertThat(authentication.getPrincipal())
        .isEqualTo(userDetails);
```

## 19. Servicios sin dependencias externas

Cuando una clase encapsule una funcionalidad determinística y no dependa de infraestructura externa (base de datos, servicios REST, mensajería, etc.), las pruebas deberán ejecutarse utilizando implementaciones reales en lugar de mocks.

Ejemplos:

* JwtService
* Utilidades de conversión
* Validadores
* Mappers

El objetivo es validar el comportamiento real del componente y evitar pruebas que únicamente verifiquen el comportamiento de los mocks.

## 19.1 Instanciación directa de la clase bajo prueba

Cuando una clase encapsule una funcionalidad determinista y no dependa de componentes externos (como repositorios, servicios, clientes HTTP o infraestructura de Spring), las pruebas deberán instanciar directamente la implementación bajo prueba.

En estos casos no deberá utilizarse:

* @SpringBootTest
* @Mock
* @InjectMocks
* Mockito

Ejemplo:

```java
private final DefaultCreditCalculator calculator =
        new DefaultCreditCalculator();
```

Este enfoque mantiene las pruebas:

* rápidas;
* aisladas;
* fáciles de comprender;
* centradas exclusivamente en el comportamiento del componente.

Las pruebas son parte del código fuente del proyecto y deberán mantener el mismo nivel de calidad, organización y claridad que el código de producción.

## 19.2 Mappers generados con MapStruct

Una prueba del mapper utiliza directamente la implementación generada con Mappers.getMapper(...), sin @SpringBootTest ni Mockito. Debe comprobar los campos relevantes y los métodos default que delegan a fábricas del dominio.

```java
private final IdentityLookupMapper mapper =
        Mappers.getMapper(IdentityLookupMapper.class);
```

No construir la respuesta a mano como sustituto de la conversión real. Si el mapper incorpora colaboradores inyectados por Spring, proporcionarlos explícitamente en su prueba; no asumir que Mappers.getMapper resolverá un grafo de dependencias Spring.

## 20 Pruebas de interfaz

Las pruebas deben cubrir edición por UUID, documento obligatorio por tipo, ceros iniciales, pegado, cambio de tipo, precisión de montos y payload sin separadores. 

La paginación verifica rangos, bordes, vacíos y reinicio al filtrar; los contadores del inventario deben conservar el conjunto completo de la sucursal. 

Mantener regresiones de permisos, estado AVAILABLE, formulario desactualizado, doble envío y respuesta perdida.

Estas pruebas de navegador usan API simulada para controlar fallos y permisos. Se acompañan de pruebas backend del contrato y PostgreSQL; no se denominan pruebas end to end con servidor real. No se añaden librerías de producción. Las nuevas pruebas backend siguen nombres should...When..., AAA, helpers privados al final y AssertJ; no usan ReflectionTestUtils ni Javadoc.

Para la consulta DNI se comprueban: ausencia de llamadas con documento incompleto o CE; ceros iniciales; autocompletado; edición sin consumo externo; cambio de documento; respuesta tardía; corrección manual durante la búsqueda; reintento explícito; cierre del modal; sesión sustituida; documento discordante en respuesta; recuperación mediante nombre manual y guardado bloqueado mientras hay búsqueda pendiente. Se conservan las regresiones previas de formularios compartidos.

## 21 Pruebas de integraciones externas

Las pruebas automatizadas de contratos externos utilizan respuestas controladas y credenciales sintéticas. No necesitan el token real del proveedor ni consumen su cuota. El tiempo se representa mediante Clock fijo cuando determina vigencia o presupuesto.

Las pruebas del caso de uso cubren caché vigente/vencida, proveedor deshabilitado, reserva de consumo, fallos externos sin persistencia y validación documental. Las del adaptador cubren JSON, documento coincidente, tamaño, estado funcional y traducción de transporte/status. MockMvc comprueba serialización, validación, permisos y códigos internos; un error de credencial externa no se transforma en un 401 de la sesión del operador.

Las consultas nativas y reservas atómicas se verifican mediante PostgreSQL en Testcontainers: upsert, auditoría, instantes bajo zona Lima, concurrencia, rollback de reservas parciales y cambio de mes. No se utiliza la base de desarrollo.

Un navegador con API simulada verifica la interfaz y sus fallos controlados; no es una prueba end to end contra Factiliza real. La disponibilidad externa y la integración operacional se verifican por separado mediante consultas autorizadas. La evidencia de entrega registra el conteo de pruebas por fecha, sin convertirlo en una regla permanente de este estándar.