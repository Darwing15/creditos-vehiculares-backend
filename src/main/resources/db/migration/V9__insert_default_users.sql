-- =====================================================
-- 1. Insertar roles
-- =====================================================
INSERT INTO roles (name, description, created_by, updated_by) VALUES
(
    'ADMIN',
    'Administrador con acceso completo al sistema',
    0,  -- se actualizará posteriormente o queda NULL
    0
),
(
    'USER',
    'Usuario estándar con permisos limitados',
    0,
    0
);

-- =====================================================
-- 2. Insertar sucursales (branches)
-- =====================================================
INSERT INTO branches (code, name, address, phone, email, is_active, created_by, updated_by) VALUES
(
    'CENT',
    'Sede Central (Tocache)',
    'Av. Ricardo Palma 202, Tocache',
    '+5111234567',
    'central@inversiones.com',
    TRUE,
    0,
    0
),
(
    'TINGO',
    'Sede Tingo María',
    'Jr. Norte 456, Tingo María',
    '+5112345678',
    'tingo@inversiones.com',
    TRUE,
    0,
    0
);

-- =====================================================
-- 3. Insertar usuarios (usando los roles y sucursales recién creados)
--    Contraseñas originales (encriptadas con BCrypt):
--      admin@inversiones.com  → "admin123"
--      user@inversiones.com   → "user123"
-- =====================================================
INSERT INTO users (
    full_name,
    email,
    password,
    role_id,
    branch_id,
    is_active,
    created_by,
    updated_by
)
SELECT
    'Administrador',
    'admin@inversiones.com',
    '$2a$12$myhFnlOLbVnUBmSGbc1KYeV2LwDe6jy.yurSiORDO2bynnVJYZL6.',
    r.id,
    b.id,
    TRUE,
    0,
    0
FROM roles r
CROSS JOIN branches b
WHERE r.name = 'ADMIN'
  AND b.code = 'CENT'

UNION ALL

SELECT
    'Usuario Demo',
    'user@inversiones.com',
    '$2a$12$EYuh4QYOxqV/CjGDwyur4earEUms..HAwpi3yOLZvbagX.uKjdEPa',
    r.id,
    b.id,
    TRUE,
    0,   -- se puede actualizar después con el id del admin
    0
FROM roles r
CROSS JOIN branches b
WHERE r.name = 'USER'
  AND b.code = 'CENT';