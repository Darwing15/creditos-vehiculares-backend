/* SOLO PRUEBAS: no forma parte del JAR ni de las migraciones de aplicación. */
INSERT INTO roles (name, description) VALUES
 ('ADMIN', 'Rol para las pruebas'), ('USER', 'Rol básico para las pruebas');
INSERT INTO branches (code, name, address, is_active) VALUES
 ('CENT', 'SUCURSAL DE PRUEBA', 'DIRECCIÓN DE PRUEBA', TRUE),
 ('TINGO', 'OTRA SUCURSAL DE PRUEBA', 'DIRECCIÓN DE PRUEBA', TRUE);
INSERT INTO users (full_name, email, password, role_id, branch_id, is_active)
 VALUES ('ADMIN DE PRUEBA', 'admin@example.invalid',
  crypt('credential-only-for-isolated-test', gen_salt('bf', 12)),
  (SELECT id FROM roles WHERE name='ADMIN'),
  (SELECT id FROM branches WHERE code='CENT'), TRUE);

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
