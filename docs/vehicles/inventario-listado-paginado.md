# Inventario: búsqueda, página y resumen

Las nuevas rutas de lectura exigen `vehicles:read`, obtienen la sucursal del principal y excluyen unidades eliminadas. La pantalla de inventario y el selector de vehículos de la solicitud de crédito usan `GET /api/vehicles/page`; el selector solicita unidades `AVAILABLE`. El catálogo `GET /api/vehicles` conserva su contrato original y su parámetro `status` por compatibilidad. Antes de retirarlo, identifica y migra cualquier consumidor restante, incluidos clientes externos.

## GET /api/vehicles/page

- `q` opcional; máximo 150 unidades UTF-16 de Java antes de recortar. Busca una subcadena literal en marca, modelo, UUID público, chasis o motor concatenados. PostgreSQL convierte a minúsculas según su configuración; las tildes deben coincidir. `%`, `_`, `\\` y comillas se buscan literalmente, no como comodines. No interpola texto en SQL.
- `brand` opcional; coincidencia exacta con la marca recibida del resumen; máximo 100 unidades UTF-16. Si se omite o es vacío, incluye todas.
- `status` opcional: `AVAILABLE`, `RESERVED`, `SOLD`; al omitirlo incluye todos.
- `page` desde cero, entre 0 y 1 000 000 (predeterminado 0); `size` entre 1 y 100 (predeterminado 10). Parámetros inválidos devuelven 400 `VALIDATION_ERROR`.

Los filtros se combinan; el orden es por identificador ascendente. La respuesta es `{items, page, size, totalElements, totalPages}`; una página fuera de rango devuelve `items` vacío y el total actual. El frontend vuelve a la última página válida si disminuye el total. El conteo, las filas y solicitudes sucesivas no conforman una instantánea continua ante escrituras concurrentes.

`q` puede contener información sensible y aparecer en registros de acceso de proxies: revisar la política de logs antes del despliegue.

## GET /api/vehicles/overview

Devuelve `{total, available, reserved, brands}` para **toda la sucursal**, sin considerar búsqueda, marca o estado de la tabla. Eso conserva la semántica anterior de los indicadores y del selector de marcas. Se consulta independientemente del cambio de filtros, y al actualizar o guardar una ficha. Si falla, la tabla aún puede consultarse y se muestra un error en el resumen. Sus marcas se ordenan en el frontend.

La lista de marcas distintas no está paginada; si su cantidad crece considerablemente, medir y diseñar otro selector. Los totales y marcas se consultan por separado y podrían variar entre consultas si hay escrituras simultáneas. No se incorporan migraciones ni se presupone que la búsqueda por subcadena use un índice; evaluar planes y volúmenes en pruebas antes de agregar índices mediante una migración nueva.

## Pruebas

`mvn clean test` usa PostgreSQL temporal con Docker. Se prueban filtros combinados, caracteres literales, paginación, totales, marcas, datos eliminados, sucursales, permisos y compatibilidad del catálogo anterior. Las pruebas aisladas usaron PostgreSQL 17, no la base real. `npm run check` y `npm run test:browser` verifican la interfaz con API simulada, respuestas tardías, fallos y cambios de página. Debe probarse también con la versión de PostgreSQL y el proxy del entorno real antes de desplegar.
