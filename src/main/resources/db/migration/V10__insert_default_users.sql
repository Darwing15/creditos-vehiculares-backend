/*
 * V10: datos iniciales EXCLUSIVOS DE DESARROLLO Y PRUEBAS LOCALES.
 *
 * Requiere V1 (pgcrypto) y la V2.
 * Ejecutar como parte de la cadena completa de Flyway en una base vacía.
 * Las contraseñas de demostración son públicas, NO secretos de producción.
 *
 * ADMIN recibe los ocho permisos actuales del módulo de seguridad.
 * USER puede autenticarse, pero no obtiene permisos de administración.
 * El nombre ADMIN se usa únicamente para identificar el rol al sembrar datos.
 * La autorización en ejecución debe basarse en role_permissions.
 *
 * NULL en created_by/updated_by representa creación por migración, no un usuario 0.
 * No se fuerzan IDs: se resuelven mediante claves únicas de roles y sucursales.
 * No se usa ON CONFLICT DO NOTHING: un estado inicial inesperado debe fallar.
 *
 * @author DarwinG
 */

-- 1. Roles iniciales. No implican permisos sobre modulos ajenos a seguridad.
INSERT INTO roles (name, description, created_by, updated_by)
VALUES
    ('ADMIN', 'Administrador con todos los permisos del módulo de seguridad', NULL, NULL),
    ('USER', 'Usuario de prueba sin permisos de administración de seguridad', NULL, NULL);

-- 2. Sucursales. manager_id queda NULL hasta asignar un responsable real.
INSERT INTO branches (
    code, name, address, phone, email, is_active, created_by, updated_by
)
VALUES
    (
        'CENT',
        'Sede Central (Tocache)',
        'Av. Ricardo Palma 202, Tocache',
        '+5111234567',
        'central@inversiones.com',
        TRUE, NULL, NULL
    ),
    (
        'TINGO',
        'Sede Tingo María',
        'Jr. Norte 456, Tingo María',
        '+5112345678',
        'tingo@inversiones.com',
        TRUE, NULL, NULL
    );

-- 3. Usuarios de demostración.
-- pgcrypto genera BCrypt variante 2a con coste 12 y sal aleatoria por contraseña.
-- Las contraseñas cumplen la política actual: >= 15 caracteres y <= 72 bytes UTF-8.
-- ronald.sotomayor@inversiones.com -> AdminLocal2026#
-- wendy.sotomayor@inversiones.com  -> UsuarioLocal2026#
-- No reutilizar estas contraseñas fuera de una instalación local de pruebas.
-- token_version comienza en 1; row_version comienza en 0 según los defaults de V2.
INSERT INTO users (
    full_name, email, password, role_id, branch_id,
    is_active, created_by, updated_by
)
VALUES
    (
        'Ronald Sotomayor',
        'ronald.sotomayor@inversiones.com',
        crypt('AdminLocal2026#', gen_salt('bf', 12)),
        (SELECT id FROM roles WHERE name = 'ADMIN'),
        (SELECT id FROM branches WHERE code = 'CENT'),
        TRUE, NULL, NULL
    ),
    (
        'Wendy Sotomayor',
        'wendy.sotomayor@inversiones.com',
        crypt('UsuarioLocal2026#', gen_salt('bf', 12)),
        (SELECT id FROM roles WHERE name = 'USER'),
        (SELECT id FROM branches WHERE code = 'CENT'),
        TRUE, NULL, NULL
    );

-- 4. Concesion explicita de los permisos del catalogo al administrador inicial.
-- No se conceden permisos al rol USER: permite comprobar respuestas 403.
INSERT INTO role_permissions (role_id, permission)
SELECT r.id, p.permission
FROM roles r
CROSS JOIN (
    VALUES
        ('security:users:read'),
        ('security:users:write'),
        ('security:users:assign-role'),
        ('security:roles:read'),
        ('security:roles:write'),
        ('security:roles:manage-permissions'),
        ('security:branches:read'),
        ('security:branches:write'),
        ('customers:read'),
        ('customers:write'),
        ('vehicles:read'),
        ('vehicles:write'),
        ('credit-applications:read'),
        ('credit-applications:create'),
        ('credit-applications:resolve')
) AS p(permission)
WHERE r.name = 'ADMIN'
  AND r.is_deleted = FALSE;

-- 5. Comprobaciones iniciales. Flyway ejecuta esta migracion transaccionalmente
-- en PostgreSQL: un error evita conservar una inicializacion parcial.
DO $security_seed$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM security_admin_guard WHERE singleton = 1
    ) THEN
        RAISE EXCEPTION 'Falta la fila de coordinación security_admin_guard.';
    END IF;

    -- La PK impide permisos duplicados y el CHECK de V2 limita el catalogo a diez.
    -- Se verifica capacidad efectiva, no el nombre del rol ni un ID fijo.
    IF NOT EXISTS (
        SELECT 1
        FROM users u
        JOIN roles r ON r.id = u.role_id
        JOIN branches b ON b.id = u.branch_id
        WHERE u.is_active = TRUE
          AND u.is_deleted = FALSE
          AND r.is_deleted = FALSE
          AND b.is_active = TRUE
          AND b.is_deleted = FALSE
          AND (
              SELECT count(*)
              FROM role_permissions rp
              WHERE rp.role_id = r.id
          ) = 15
    ) THEN
        RAISE EXCEPTION
            'La inicialización requiere un administrador activo con los quince permisos de seguridad y una sucursal activa.';
    END IF;
END;
$security_seed$;