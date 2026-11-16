# Listado paginado de solicitudes

`GET /api/credit-applications/page` devuelve solicitudes de la sucursal autenticada y requiere `credit-applications:read`. El listado anterior `GET /api/credit-applications?status=...` permanece disponible para consumidores existentes; no se retira en este brick.

## Parámetros y respuesta

- `status` es obligatorio: `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED` o `EXPIRED`.
- `q` es opcional y admite hasta 150 unidades UTF-16 de Java antes de recortar. Busca por subcadena literal en número, nombre visible de cliente, descripción visible de vehículo y etiquetas de identificador `Cliente #...` y `Vehículo #...`. Se ignoran mayúsculas/minúsculas según la configuración de PostgreSQL; las tildes deben coincidir. `%`, `_`, `\\` y comillas son literales. El texto se enlaza como parámetro.
- `page` comienza en cero, por defecto 0, máximo 1 000 000; `size` por defecto 10, mínimo 1 y máximo 100. Los valores incorrectos reciben 400 `VALIDATION_ERROR`. El orden es por identificador de solicitud ascendente.
- Respuesta `{items, page, size, totalElements, totalPages}`. Cada elemento contiene `{application, customerName, vehicleName}`; `application` es el DTO actual. Los nombres pueden ser nulos. Para una página fuera de rango `items` es vacío y se conservan los totales.

La búsqueda de nombre y el total tienen las mismas restricciones de permisos que la respuesta. La sucursal y los permisos se obtienen en el servidor, nunca de parámetros del navegador. El resultado excluye solicitudes eliminadas; las dos consultas de nombres se limitan a los IDs presentes en la página.

## Permisos y datos históricos

| Permiso adicional | Dato visible y buscable |
| --- | --- |
| `customers:read` | Nombre del cliente de la sucursal si está activo y no eliminado. |
| `vehicles:read` | Tipo, marca y modelo del vehículo de la sucursal si no está eliminado. |

Sin el permiso, cuando falta el registro, no cumple esas condiciones o pertenece a otra sucursal, el nombre correspondiente es nulo y el frontend muestra su identificador. Los filtros tampoco buscan nombres ocultos: una consulta por un nombre no puede revelar su existencia por el número de resultados. Esto conserva la política de la pantalla anterior; **no es un nombre histórico inmutable**. Si se desea mostrar nombres de clientes inactivos o eliminados, primero debe acordarse el permiso y la fuente histórica adecuados.

Se permite buscar por los identificadores que ya aparecen en la solicitud. La consulta y las cargas de nombres son operaciones separadas, por lo que una modificación concurrente puede alterar los datos entre ellas. El paginado por desplazamiento tampoco representa una instantánea entre solicitudes.

## Interfaz, operación y pruebas

La tabla y el contador usan los filtros y total del servidor; al buscar o cambiar estado vuelve a página cero. Se cancelan o descartan respuestas anteriores; si la última página desaparece se consulta la última válida. La ventana de revisión sigue consultando el detalle antes de aprobar y recibe los nombres visibles capturados al abrirla. Los selectores de creación siguen usando los endpoints paginados de clientes e inventario.

`q` puede contener datos personales y aparecer en logs de acceso de proxy; revisar la política de registros. No se agrega migración ni índice: medir la consulta con volúmenes representativos antes de decidir si requiere uno. La prueba automatizada usa PostgreSQL 17 temporal con Docker; probar adicionalmente la versión del servidor y el proxy reales antes de desplegar.
