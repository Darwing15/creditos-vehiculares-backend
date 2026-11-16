# Inicialización y despliegue

Este procedimiento usa Java 21, PostgreSQL, Flyway y el JAR del backend. El servidor web no crea usuarios de demostración automáticamente. En una base nueva, la V10 no inserta usuarios, roles ni sucursales. El primer administrador se crea mediante una herramienta de consola, de forma explícita, o se conservan los usuarios al restaurar los datos existentes.

## Atención a bases existentes

La V10 de esta versión es diferente de la V10 anterior. **El JAR nuevo no debe iniciarse contra una base que ya tenga aplicada la V10 anterior**: Flyway detectará la diferencia de checksum. La guía de transición prepara una base nueva separada y conserva la anterior. No usar `flyway repair` para ocultar esa diferencia. Revisa también cualquier otro entorno o copia donde se haya aplicado la V10 anterior.

Las credenciales de demostración que estaban en las fuentes deben considerarse conocidas, incluso si se borra el archivo: podrían permanecer en el historial de Git, JAR o respaldos. Cambia las contraseñas de las cuentas afectadas o desactiva las que no deban usarse, antes de generar el respaldo a restaurar. Conserva al menos un administrador operativo con todos los permisos. No imprimas, compartas ni pongas las contraseñas en argumentos, historial de shell o archivos de ejemplo.

## Preparativos de seguridad

* Verifica la base, el servidor y el usuario de PostgreSQL con acceso para crear el esquema y aplicar las migraciones. Utiliza una base distinta por entorno y cuentas individuales de aplicación.
* Guarda los secretos reales fuera de los repositorios, con acceso restringido. La configuración actual recibe `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET_BASE64`; configura `FRONTEND_ORIGINS` con los orígenes reales y conserva la documentación de API deshabilitada cuando corresponda. Variables de entorno no son un almacén de secretos: los procesos y administradores con permisos suficientes pueden leerlas.
* Usa TLS validado cuando las conexiones salgan del equipo, tanto hacia la API como hacia PostgreSQL; configura los parámetros TLS apropiados para tu entorno. No incluyas usuario ni contraseña en la URL JDBC.
* Las credenciales de `src/test` son exclusivamente ficticias y están fuera de las migraciones de aplicación y del JAR. No las utilices para ingresar a ninguna instalación.

## Cambiar la clave de una cuenta existente

Con el backend anterior funcionando y antes de detener escritores o hacer el respaldo:

```bash
python3 scripts/inicializacion/cambiar_clave.py
```

La herramienta pregunta por el origen de la API, el correo, la contraseña actual y la nueva. Las claves se ingresan sin eco y no se pasan en argumentos. Acepta HTTPS con certificados verificados o HTTP únicamente en `127.0.0.1`/`::1`; no sigue redirecciones ni usa el proxy heredado. La contraseña nueva debe cumplir la política existente: 15 a 72 caracteres y como máximo 72 bytes UTF-8. El endpoint de la aplicación invalida las sesiones anteriores al cambiar la clave. Ejecuta el procedimiento para cada cuenta que deba conservarse y siga usando una clave conocida.

Si la conexión falla después de enviar el cambio, el resultado puede ser incierto: comprueba si la clave nueva funciona antes de reintentar. El programa protege los argumentos y la salida, pero las credenciales deben existir brevemente en la memoria del proceso y transitan hacia la API; evita capturar el cuerpo de las solicitudes en proxy o registros.

## A. Instalación completamente nueva

1. Crea una base de datos **nueva y sin datos** con el propietario y permisos acordados. No borres ni reutilices otra base. Compila el backend con `mvn clean package`; las pruebas de integración requieren Docker y PostgreSQL de prueba.
2. Desde una terminal interactiva ejecuta:

   ```bash
   JAR='target/creditos-vehiculares-backend-1.0.1.jar'
   java -Dloader.main=pe.sotomayor.creditos.bootstrap.InitialAdminBootstrap \
     -cp "$JAR" org.springframework.boot.loader.launch.PropertiesLauncher
   ```

3. Elige `INICIALIZAR`. Indica la URL JDBC sin credenciales y el usuario de PostgreSQL; introduce su contraseña sin eco. El programa muestra el nombre de la base y el servidor, y solicita escribir el nombre de la base como confirmación. Verifica ambos antes de continuar.
4. Introduce los datos reales de la sucursal y del primer administrador; elige y confirma una contraseña nueva. Se aplican las validaciones de los DTO habituales y se crea el hash BCrypt localmente. La base recibe el hash, no la contraseña en texto claro. Se crean un rol administrativo con los 15 permisos actuales, una sucursal y una cuenta, en una transacción.
5. La herramienta rechaza bases con datos de aplicación. Si cancelas después de crear el esquema pero antes de guardar el administrador, puedes ejecutar `INICIALIZAR` nuevamente sobre esa base preparada: verifica Flyway y que no existan datos. Si hubo una interrupción después de enviar la operación, revisa el estado antes de reintentar. No emplees este camino para añadir otro administrador a una base con datos.
6. Configura el servicio con secretos propios del entorno, inicia el backend y comprueba el acceso y los permisos. Crea las demás cuentas mediante las funciones disponibles de la aplicación y asigna únicamente los permisos necesarios.

## B. Transición conservando los datos actuales

Este camino prepara una **segunda base** en el mismo servidor PostgreSQL. Conserva los datos de `public`, incluidos usuarios, roles, permisos, sucursales, clientes, vehículos, datos de identidad, créditos y secuencias. La base original no se modifica ni elimina. El procedimiento no contempla datos de otros esquemas ni cambios manuales de esquema que no estén en las migraciones; identifícalos y detente si existen.

### 1. Rotar claves y disponer de respaldo

Con el servicio actual en funcionamiento, cambia las claves conocidas como se indica arriba. Luego coordina una ventana de mantenimiento y detén **todos** los procesos que escriben o consumen secuencias de la base de origen, incluidos el backend, trabajos adicionales y sus conexiones. El servicio anterior y los escritores deben permanecer detenidos hasta terminar la comparación o decidir volver a la base original.

Genera y comprueba un **respaldo completo independiente y actualizado** mediante tu procedimiento habitual. Mantén una copia protegida aparte del archivo temporal de transferencia. No continúes sin verificar el respaldo. La restauración de abajo necesita una cuenta PostgreSQL superusuario para deshabilitar temporalmente disparadores de integridad y validarlos después.

### 2. Identificar origen y crear destino

Sustituye los tres valores por los nombres reales verificados; origen y destino deben ser bases distintas del mismo servidor local. Comprueba que el destino todavía no exista y que `DUENO_DB` sea el propietario acordado, antes de crearlo. Usa `template0` para no heredar datos ajenos:

```bash
ORIGEN='REEMPLAZAR_BASE_ORIGEN'
DESTINO='REEMPLAZAR_BASE_NUEVA'
DUENO_DB='REEMPLAZAR_ROL_DUENO'
HERRAMIENTAS="$PWD/scripts/inicializacion"

sudo -u postgres psql -h /var/run/postgresql -X -d "$ORIGEN" \
  -c "SELECT current_database(), pid, usename, application_name, state FROM pg_stat_activity WHERE datname=current_database() AND pid<>pg_backend_pid();"

test -n "$ORIGEN" && test -n "$DESTINO" && test "$ORIGEN" != "$DESTINO" && \
  sudo -u postgres createdb -h /var/run/postgresql --template=template0 --owner="$DUENO_DB" "$DESTINO"
```

Identifica cualquier conexión restante y su proceso; no cierres sesiones ajenas sin coordinación. Verifica también que no haya datos de aplicación fuera de `public`.

### 3. Preparar únicamente el esquema nuevo

Ejecuta el comando de consola de la sección A y elige **`PREPARAR`**. Conéctate a `DESTINO`, verifica el servidor y confirma su nombre. Aplica las migraciones, pero no crea administrador, roles ni sucursales. No ejecutes `INICIALIZAR` en esta ruta: esos datos se restaurarán con sus ID originales.

Confirma que el esquema en origen y destino coincide antes de copiar datos. Los siguientes archivos se crean en un directorio temporal privado del usuario y contienen solo definiciones de esquema; aun así, trátalos como información interna. Se omiten propietarios y privilegios en esta comparación, por lo que deben verificarse por separado antes del cambio de servicio.

```bash
umask 077
REVISION="$(mktemp -d /tmp/creditos-revision-XXXXXX)"
sudo -u postgres pg_dump -h /var/run/postgresql --schema-only --schema=public --no-owner --no-privileges -d "$ORIGEN" > "$REVISION/origen.sql"
sudo -u postgres pg_dump -h /var/run/postgresql --schema-only --schema=public --no-owner --no-privileges -d "$DESTINO" > "$REVISION/destino.sql"
sed '/^\\restrict /d; /^\\unrestrict /d' "$REVISION/origen.sql" > "$REVISION/origen-normalizado.sql"
sed '/^\\restrict /d; /^\\unrestrict /d' "$REVISION/destino.sql" > "$REVISION/destino-normalizado.sql"
diff -u "$REVISION/origen-normalizado.sql" "$REVISION/destino-normalizado.sql"
```

Si `diff` encuentra cambios, detente y examina las diferencias; no restaures sobre un esquema incompatible. Contrasta por separado el propietario, los GRANT, las configuraciones de la base, extensiones y cualquier política local; `--no-owner --no-privileges` los excluye de la comparación. No actualices `flyway_schema_history` del destino.

### 4. Exportar y restaurar solo en el destino

Comprueba otra vez que todos los escritores y consumidores de secuencias estén detenidos. Este respaldo de transferencia contiene datos sensibles: se crea bajo un directorio temporal privado del usuario `postgres`. No sustituye el respaldo completo independiente.

```bash
DIR_PG="$(sudo -u postgres mktemp -d /tmp/creditos-datos-XXXXXX)"
sudo -u postgres psql -h /var/run/postgresql -X -qAt -d "$ORIGEN" < "$HERRAMIENTAS/inventario.sql" > "$REVISION/inventario-origen.txt"
sudo -u postgres pg_dump -h /var/run/postgresql --format=custom --data-only --schema=public \
  --exclude-table-data=public.flyway_schema_history \
  --exclude-table-data=public.security_admin_guard \
  --file="$DIR_PG/datos.dump" --dbname="$ORIGEN"
sudo -u postgres pg_restore --list "$DIR_PG/datos.dump" > "$REVISION/contenido-respaldo.txt"
```

Revisa la lista y confirma que se incluyan los datos y estados de secuencia esperados, incluidos los de numeración comercial. Se excluyen los datos del historial Flyway y de la fila singleton de control: la base nueva ya tiene sus propias versiones correctas. Si cualquier paso falla, detente.

Antes de cargar, ejecuta el preflight: rechaza un destino con datos y comprueba que su nombre no coincida con el origen. No alteres ni elimines estas comprobaciones.

```bash
sudo -u postgres psql -h /var/run/postgresql -X -v ON_ERROR_STOP=1 \
  -v origen="$ORIGEN" -v destino="$DESTINO" -d "$DESTINO" < "$HERRAMIENTAS/verificar_destino.sql"
```

Solo si el preflight y la comparación de esquema terminaron correctamente, carga el respaldo:

```bash
sudo -u postgres pg_restore -h /var/run/postgresql --data-only --disable-triggers \
  --single-transaction --no-owner --no-acl --dbname="$DESTINO" "$DIR_PG/datos.dump"
```

La desactivación temporal de disparadores permite restaurar la relación circular entre sucursal y responsable. Las restricciones de claves foráneas deben comprobarse después: volver a habilitar disparadores no valida por sí solo las filas existentes. PostgreSQL requiere superusuario para esta opción. Si falla o se interrumpe, conserva el origen intacto y descarta el destino de pruebas antes de volver a prepararlo: las secuencias pueden cambiar aunque la transacción falle.

### 5. Validar antes de usar el destino

```bash
sudo -u postgres psql -h /var/run/postgresql -X -v ON_ERROR_STOP=1 \
  -d "$DESTINO" < "$HERRAMIENTAS/validar_restauracion.sql"
sudo -u postgres psql -h /var/run/postgresql -X -qAt -d "$DESTINO" \
  < "$HERRAMIENTAS/inventario.sql" > "$REVISION/inventario-destino.txt"
diff -u "$REVISION/inventario-origen.txt" "$REVISION/inventario-destino.txt"
```

La validación comprueba los disparadores internos, las claves foráneas del esquema actual, las secuencias de ID y la existencia de un administrador activo con el catálogo completo. Si detecta una forma nueva de clave foránea que no sabe validar, se detiene. La comparación cubre conteos y valores de secuencias, incluidas las de numeración comercial; **no prueba que cada campo de cada fila sea idéntico**. Compara además una muestra representativa de clientes, vehículos, sucursales, responsables y auditoría entre origen y destino, y prueba el inicio de sesión con las claves nuevas.

Verifica propietarios, permisos, parámetros de base y conexión/TLS. Prueba el JAR nuevo y el frontend en un entorno controlado apuntando al destino: autenticación, listado y registro de clientes y vehículos, y simulación de crédito. No uses credenciales reales en scripts de prueba ni logs. Si cualquier verificación falla, no cambies el servicio y conserva la base anterior y el respaldo completo. Solo después de que el responsable valide el resultado, programa el cambio de conexión del servicio y conserva el procedimiento de retorno.

## Verificaciones de desarrollo

```bash
mvn clean test
python3 -m unittest discover -s scripts/inicializacion -p 'test_*.py' -v
```

Las pruebas de migración e inicialización usan PostgreSQL temporal y datos ficticios. El procedimiento de restauración se ensaya con una sucursal que tiene un responsable, varios usuarios, clientes, vehículos, caché y secuencias; no se conecta a la base real. Las pruebas automatizadas no sustituyen la comparación de tus datos ni la validación del despliegue.
