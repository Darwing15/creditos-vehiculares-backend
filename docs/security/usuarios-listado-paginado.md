# Listado paginado de usuarios

`GET /api/security/users/page` requiere `security:users:read`. El alcance es global, como el listado anterior: incluye usuarios inactivos y excluye los eliminados lógicamente. El backend ordena por ID ascendente para que la paginación sea estable mientras no cambien los datos.

Parámetros: `q` opcional (máximo 150 unidades UTF-16; se recortan extremos), `roleId` y `branchId` opcionales positivos, `page` desde 0 hasta 1 000 000 y `size` entre 1 y 100. La búsqueda es un fragmento literal sin distinción de mayúsculas sobre nombre, correo y nombre de rol. La respuesta contiene `items`, `page`, `size`, `totalElements` y `totalPages`. El frontend solicita 10 por página.

`GET /api/security/users/filter-options` requiere el mismo permiso y devuelve `roles` y `branches` con `{id, name}` distintos vinculados a usuarios no eliminados, incluidos los inactivos. Esto conserva las opciones que antes se obtenían del listado completo, incluso para quien carece de permisos de lectura de catálogos de roles o sucursales. Esas opciones tienen el mismo alcance global que el listado de usuarios; no son un catálogo administrativo completo.

El listado anterior `GET /api/security/users` sigue disponible. Las operaciones de escritura mantienen sus permisos y contratos. No se añade migración ni índice. La cuenta y la página pueden variar con escrituras concurrentes: no se promete una instantánea entre solicitudes; el frontend vuelve a la página anterior si la última queda vacía. No se han medido los planes de ejecución con datos reales. Los textos de búsqueda viajan en la URL y pueden aparecer en registros de proxy; evitar registrar cadenas de consulta si contienen datos personales.
