/*
 * V2: estructura inicial del módulo de seguridad.
 *
 * row_version: bloqueo optimista de Hibernate (@Version).
 * token_version: revocación de JWT del usuario; es independiente de row_version.
 * role_permissions: autoridades explícitas, sin privilegios por nombre del rol.
 * security_admin_guard: fila usada por SecurityWriteGate con SELECT FOR UPDATE.
 *
 * Los registros iniciales y la comprobación del administrador se cargan en V10.
 * Las reglas de autorización y del último administrador siguen en los servicios.
 *
 * @author DarwinG
 */

-- ==========================================================
-- TABLE: roles
-- Description:
-- Stores the system roles used for authorization.
-- ==========================================================

CREATE TABLE roles (

    id              BIGSERIAL PRIMARY KEY,

    name            VARCHAR(100) NOT NULL,
    description     TEXT,

    created_by      BIGINT,
    updated_by      BIGINT,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at      TIMESTAMPTZ,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    row_version     BIGINT NOT NULL DEFAULT 0 CHECK (row_version >= 0),

    CONSTRAINT uk_roles_name
        UNIQUE (name),

    CONSTRAINT chk_roles_soft_delete
        CHECK (
            (deleted_at IS NULL AND is_deleted = FALSE)
            OR
            (deleted_at IS NOT NULL AND is_deleted = TRUE)
        )
);

-- ==========================================================
-- ROLES COMMENTS
-- ==========================================================

COMMENT ON TABLE roles IS
'Stores system roles used for authorization.';

COMMENT ON COLUMN roles.id IS
'Primary key of the role.';

COMMENT ON COLUMN roles.name IS
'Unique role name.';

COMMENT ON COLUMN roles.description IS
'Description of the role responsibilities.';

COMMENT ON COLUMN roles.created_by IS
'Creator user identifier; NULL for the initial Flyway seed.';

COMMENT ON COLUMN roles.updated_by IS
'Last editor user identifier; NULL for the initial Flyway seed.';

COMMENT ON COLUMN roles.created_at IS
'Creation timestamp.';

COMMENT ON COLUMN roles.updated_at IS
'Last update timestamp.';

COMMENT ON COLUMN roles.deleted_at IS
'Logical deletion timestamp.';

COMMENT ON COLUMN roles.is_deleted IS
'Logical deletion flag.';

-- ==========================================================
-- TABLE: branches
-- Description:
-- Stores company branches. Every operational module is scoped
-- to a branch (users, customers, vehicles, credits, payments).
-- IMPORTANT:
-- branches must be created before users because users
-- references branches(id)
-- ==========================================================

CREATE TABLE branches (

    id              BIGSERIAL PRIMARY KEY,

    code            VARCHAR(20) NOT NULL,

    name            VARCHAR(100) NOT NULL,

    address         VARCHAR(255) NOT NULL,

    phone           VARCHAR(20),

    email           VARCHAR(120),

    -- Manager user ID.
    -- The foreign key is added after the users table is created
    -- within this same migration to resolve the creation dependency.
    manager_id      BIGINT,

    is_active       BOOLEAN NOT NULL DEFAULT TRUE,

    created_by      BIGINT,

    updated_by      BIGINT,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at      TIMESTAMPTZ,

    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    row_version     BIGINT NOT NULL DEFAULT 0 CHECK (row_version >= 0),

    CONSTRAINT uk_branches_code
        UNIQUE (code),

    CONSTRAINT chk_branch_code_not_empty
        CHECK (length(trim(code)) > 0),

    CONSTRAINT chk_branch_name_not_empty
        CHECK (length(trim(name)) > 0),

    CONSTRAINT chk_branch_address_not_empty
        CHECK (length(trim(address)) > 0),

    CONSTRAINT chk_branches_soft_delete
        CHECK (
            (deleted_at IS NULL AND is_deleted = FALSE)
            OR
            (deleted_at IS NOT NULL AND is_deleted = TRUE)
        )
);

-- ==========================================================
-- BRANCHES COMMENTS
-- ==========================================================

COMMENT ON TABLE branches IS
'Stores company branches where business operations are performed.';

COMMENT ON COLUMN branches.id IS
'Primary key of the branch.';

COMMENT ON COLUMN branches.code IS
'Unique business code of the branch.';

COMMENT ON COLUMN branches.name IS
'Business name of the branch.';

COMMENT ON COLUMN branches.address IS
'Physical address of the branch.';

COMMENT ON COLUMN branches.phone IS
'Contact phone number.';

COMMENT ON COLUMN branches.email IS
'Contact email address.';

COMMENT ON COLUMN branches.manager_id IS
'Identifier of the user assigned as branch manager.';

COMMENT ON COLUMN branches.is_active IS
'Indicates whether the branch is available for business operations.';

COMMENT ON COLUMN branches.created_by IS
'Creator user identifier; NULL for the initial Flyway seed.';

COMMENT ON COLUMN branches.updated_by IS
'Last editor user identifier; NULL for the initial Flyway seed.';

COMMENT ON COLUMN branches.created_at IS
'Creation timestamp.';

COMMENT ON COLUMN branches.updated_at IS
'Last update timestamp.';

COMMENT ON COLUMN branches.deleted_at IS
'Logical deletion timestamp.';

COMMENT ON COLUMN branches.is_deleted IS
'Logical deletion flag.';

-- ==========================================================
-- INDEXES
-- ==========================================================

-- Queries:
-- SELECT * FROM branches
-- WHERE is_active = true
--   AND is_deleted = false;
CREATE INDEX idx_branches_active
ON branches(is_active)
WHERE is_deleted = FALSE;

-- Queries:
-- SELECT * FROM branches
-- WHERE manager_id = ?;
CREATE INDEX idx_branches_manager
ON branches(manager_id)
WHERE is_deleted = FALSE;

-- ==========================================================
-- TABLE: users
-- Description:
-- Stores authenticated users of the credit management system.
-- ==========================================================

CREATE TABLE users (
    id BIGSERIAL,

    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(120) NOT NULL,
    password VARCHAR(255) NOT NULL,
    token_version BIGINT NOT NULL DEFAULT 1 CHECK (token_version >= 1),

    role_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_by BIGINT,
    updated_by BIGINT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    row_version BIGINT NOT NULL DEFAULT 0 CHECK (row_version >= 0),

    CONSTRAINT pk_users
        PRIMARY KEY (id),

    CONSTRAINT uk_users_email
        UNIQUE (email),

    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id),

    CONSTRAINT fk_users_branch
        FOREIGN KEY (branch_id)
        REFERENCES branches(id),

    CONSTRAINT chk_users_full_name_not_empty
        CHECK (length(trim(full_name)) > 0),

    CONSTRAINT chk_users_email_not_empty
        CHECK (length(trim(email)) > 0),

    CONSTRAINT chk_users_password_not_empty
        CHECK (length(trim(password)) > 0),

    CONSTRAINT chk_users_soft_delete
        CHECK (
            (deleted_at IS NULL AND is_deleted = FALSE)
            OR
            (deleted_at IS NOT NULL AND is_deleted = TRUE)
        )
);

-- ==========================================================
-- USERS INDEXES
-- ==========================================================

CREATE INDEX idx_users_role
    ON users(role_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_users_branch
    ON users(branch_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_users_branch_active
    ON users(branch_id, is_active)
    WHERE is_deleted = FALSE;

-- ==========================================================
-- USERS COMMENTS
-- ==========================================================

COMMENT ON TABLE users IS
'Stores authenticated users of the credit management system.';

COMMENT ON COLUMN users.id IS
'Primary key of the user.';

COMMENT ON COLUMN users.full_name IS
'Full name of the employee.';

COMMENT ON COLUMN users.email IS
'Unique email used for authentication.';

COMMENT ON COLUMN users.password IS
'BCrypt password hash; not reversible encryption.';

COMMENT ON COLUMN users.role_id IS
'Reference to the assigned role.';

COMMENT ON COLUMN users.branch_id IS
'Reference to the assigned branch.';

COMMENT ON COLUMN users.is_active IS
'Indicates whether the user is allowed to authenticate.';

COMMENT ON COLUMN users.created_by IS
'Creator user identifier; NULL for the initial Flyway seed.';

COMMENT ON COLUMN users.updated_by IS
'Last editor user identifier; NULL for the initial Flyway seed.';

COMMENT ON COLUMN users.created_at IS
'Creation timestamp.';

COMMENT ON COLUMN users.updated_at IS
'Last update timestamp.';

COMMENT ON COLUMN users.deleted_at IS
'Logical deletion timestamp.';

COMMENT ON COLUMN users.is_deleted IS
'Logical deletion flag.';

/*=========================================================
 BRANCH MANAGER RELATIONSHIP

 Branches are created before users because every user
 belongs to a branch.

 Once users exists, the optional manager relationship can
 be established without introducing a separate migration.
=========================================================*/

ALTER TABLE branches
    ADD CONSTRAINT fk_branches_manager
        FOREIGN KEY (manager_id)
        REFERENCES users(id);

-- ==========================================================
-- CONCURRENCIA Y REVOCACION
-- ==========================================================

COMMENT ON COLUMN roles.row_version IS
'Optimistic locking version managed by Hibernate @Version.';

COMMENT ON COLUMN branches.row_version IS
'Optimistic locking version managed by Hibernate @Version.';

COMMENT ON COLUMN users.row_version IS
'Optimistic locking version managed by Hibernate @Version; not a JWT version.';

COMMENT ON COLUMN users.token_version IS
'JWT revocation version. Compare with claim tv; increment to revoke prior tokens.';

-- ==========================================================
-- IDENTIDAD DE ACCESO
-- ==========================================================

-- Evita identidades duplicadas por mayusculas o espacios externos ordinarios.
-- Se aplica tambien a usuarios eliminados logicamente: no reutilizar su identidad.
-- No transforma el valor persistido. Normalizar correo al crear/editar/autenticar
-- sigue siendo responsabilidad de la aplicacion.
-- Se conserva uk_users_email para mantener la restriccion del modelo actual.
CREATE UNIQUE INDEX uk_users_email_normalized
    ON users (lower(btrim(email)));

COMMENT ON INDEX uk_users_email_normalized IS
'Global uniqueness for the email authentication identifier after lower/btrim.';

-- ==========================================================
-- PERMISOS EXPLICITOS DEL ROL
-- ==========================================================

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission VARCHAR(100) NOT NULL,

    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission),

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles(id),

    -- Mantener sincronizado con SecurityPermission.
    -- Ampliar el catálogo exige una nueva migración cuando exista una BD estable.
    CONSTRAINT chk_role_permissions_catalog CHECK (permission IN (
        'security:users:read',
        'security:users:write',
        'security:users:assign-role',
        'security:roles:read',
        'security:roles:write',
        'security:roles:manage-permissions',
        'security:branches:read',
        'security:branches:write',
        'customers:read',
        'customers:write',
        'vehicles:read',
        'vehicles:write',
        'credit-applications:read',
        'credit-applications:create',
        'credit-applications:resolve'
    ))
);

COMMENT ON TABLE role_permissions IS
'Explicit role authorities mapped by Role.permissions; missing authority means no grant.';

COMMENT ON COLUMN role_permissions.role_id IS
'Role receiving the authority. The composite primary key prevents duplicate grants.';

COMMENT ON COLUMN role_permissions.permission IS
'Exact stable authority code from SecurityPermission; evaluated by Spring Security.';

-- La PK comienza por role_id y ya cubre las consultas de permisos de un rol.
-- No agregar otro indice identico ni borrado en cascada: el modelo usa soft delete.

-- ==========================================================
-- SERIALIZACION DE CAMBIOS ADMINISTRATIVOS
-- ==========================================================

CREATE TABLE security_admin_guard (
    singleton SMALLINT NOT NULL,

    CONSTRAINT pk_security_admin_guard PRIMARY KEY (singleton),
    CONSTRAINT chk_security_admin_guard_singleton CHECK (singleton = 1)
);

COMMENT ON TABLE security_admin_guard IS
'Coordination row locked by SecurityWriteGate in a transaction before administrative writes.';

COMMENT ON COLUMN security_admin_guard.singleton IS
'Only value 1 is allowed. Application code must preserve this coordination row.';

-- La restriccion permite como maximo una fila; no impide eliminarla.
-- La aplicacion debe conservarla y fallar de forma cerrada si no la encuentra.
INSERT INTO security_admin_guard (singleton) VALUES (1);