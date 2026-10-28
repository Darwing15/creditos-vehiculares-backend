# Módulo Customers

Administra fichas de clientes de la sucursal autenticada y consulta nombres asociados a DNI para asistir el alta.

## Responsabilidades

Registrar, listar, consultar y editar fichas; conservar identidad documental y unicidad global; permitir obtención de nombres como operación separada. Actualizar una ficha no altera automáticamente condiciones de solicitudes o contratos existentes.

## Entidades y valores

- Customer: ficha comercial con UUID público, sucursal, documento y nombre confirmado.
- IdentityDocumentCache: último resultado reutilizable de un DNI, con auditoría y vigencia local, independiente de la ficha.
- IdentityData: valor normalizado de documento/nombres/apellidos/proveedor.

identity_lookup_usage es un contador técnico de infraestructura, no una entidad comercial ni un historial de clientes.

## Casos de uso

CreateCustomer, UpdateCustomer, GetCustomer y ListCustomers administran la ficha. LookupCustomerIdentity obtiene nombres mediante caché o Factiliza sin registrar un cliente ni reservar su documento.

El alta valida DNI de ocho dígitos ASCII; editar conserva tipo/número. El nombre continúa editable. La consulta no es obligatoria para guardar después de un fallo, porque el formulario permite nombre manual.

## Dependencias y seguridad

El módulo utiliza contexto autenticado de usuario/sucursal y Clock del sistema. La ruta de consulta requiere customers:write; la página necesita también customers:read. La caché se comparte entre sucursales de esta empresa, sin exponer fichas comerciales de otra sucursal.

El proveedor se invoca desde el backend con una credencial externa; nunca se entrega su token al navegador. Las llamadas se realizan fuera de una transacción larga. Presupuesto y upsert conservan atomicidad mediante PostgreSQL.

## Mapeos y pruebas

Los mappers usan interfaces MapStruct. IdentityLookupMapper conserva toEntry como método default de fábrica y genera toResponse. Las pruebas utilizan la implementación real generada; un spy de la interfaz sin inicializar no sustituye esa implementación.

## Documentación técnica

[Contrato, parámetros, persistencia y operación de Factiliza](../../../../../../../docs/customers/factiliza-dni.md)

La consulta no demuestra titularidad, vigencia documental ni aprobación crediticia. Expiración no implica purga automática y la coordinación actual de solicitudes duplicadas se limita a una JVM.
