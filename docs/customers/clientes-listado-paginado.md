# Listado paginado de clientes

`GET /api/customers/page` alimenta la pantalla de clientes y el selector de clientes de la solicitud de crédito. `GET /api/customers` conserva el catálogo completo sin paginar para compatibilidad. Antes de retirarlo, identifica y migra cualquier consumidor restante, incluidos clientes externos.

## Contrato

Requiere sesión y `customers:read`. La sucursal se obtiene del principal; no se acepta como filtro de autorización. Solo devuelve clientes activos y no eliminados de esa sucursal, como el catálogo anterior.

- `q`: opcional, máximo 150 unidades UTF-16 de Java antes de recortar espacios. Coincidencia por subcadena sobre nombre, documento, teléfono y dirección concatenados, sin distinguir mayúsculas de minúsculas según la conversión/collation de PostgreSQL. Las tildes deben coincidir. `%`, `_`, `\\` y comillas son caracteres literales, no comodines. El parámetro se enlaza, no se interpola en SQL. Puede aparecer en registros de acceso: evita registrar datos personales completos en proxies y monitoreo.
- `page`: opcional, índice desde cero; predeterminado 0, mínimo 0 y máximo 1 000 000.
- `size`: opcional, predeterminado 10, mínimo 1 y máximo 100.
- Si los números no se pueden convertir o los límites son inválidos, el servidor devuelve 400 con `VALIDATION_ERROR`. No hay parámetro de orden libre.

Respuesta:

```json
{
  "items": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0
}
```

Los elementos usan `CustomerOptionResponse`. El orden es por identificador ascendente, único y estable. Una página posterior al final devuelve `items` vacío y los totales actuales. El frontend regresa a la última página si descubre que el total bajó. El conteo y la consulta de filas no garantizan una instantánea continua ante escrituras concurrentes; entre solicitudes los registros pueden desplazarse.

La búsqueda de texto y el conteo se ejecutan en la base antes de paginar. No se incluye migración ni índice nuevo; evaluar con volúmenes representativos y planes de ejecución antes de introducir índices específicos para búsquedas por subcadena. El selector de solicitudes también consulta páginas; la ruta completa sigue disponible para consumidores que aún la necesiten.

## Interfaz

Clientes solicita páginas de diez, reinicia a la primera al buscar y espera 300 ms antes de solicitar. Cancela o ignora respuestas antiguas, oculta resultados de otra sesión o búsqueda y muestra fallos sin presentarlos como lista vacía. Para actualizar o después de guardar una ficha vuelve a consultar. El selector de solicitudes utiliza el contrato paginado; el catálogo anterior permanece disponible por compatibilidad.

## Verificación

`mvn clean test` ejecuta las pruebas del backend con PostgreSQL temporal de Testcontainers (requiere Docker). Se verifican límites, compatibilidad, caracteres literales, sucursal, eliminación lógica, estados, permisos, respuesta HTTP y paginación. En el frontend, `npm run check` y `npm run test:browser` verifican interfaz y respuestas simuladas, incluidas respuestas tardías. PostgreSQL de prueba es versión 17; verificar también en la versión y configuración real antes del despliegue.
