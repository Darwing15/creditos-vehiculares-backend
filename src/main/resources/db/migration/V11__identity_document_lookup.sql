/* Consulta DNI Factiliza: resultados minimos y contadores tecnicos de consumo.
 * No copia direccion, sexo, nacimiento ni el cuerpo externo completo.
 * @author DarwinG
 */
CREATE TABLE identity_document_cache (
    id BIGSERIAL PRIMARY KEY,
    document_number VARCHAR(8) NOT NULL UNIQUE,
    given_names VARCHAR(150) NOT NULL,
    first_surname VARCHAR(150),
    second_surname VARCHAR(150),
    full_name VARCHAR(150) NOT NULL,
    provider VARCHAR(30) NOT NULL,
    consulted_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    CONSTRAINT chk_identity_cache_dni
    CHECK (document_number ~ '^[0-9]{8}$'),
    CONSTRAINT chk_identity_cache_names
    CHECK (BTRIM(given_names) <> '' AND BTRIM(full_name) <> ''),
    CONSTRAINT chk_identity_cache_provider
    CHECK (provider = 'FACTILIZA'),
    CONSTRAINT chk_identity_cache_expiration
    CHECK (expires_at > consulted_at)
);
CREATE INDEX idx_identity_cache_expiration ON identity_document_cache (expires_at);


COMMENT ON TABLE identity_document_cache IS
'Stores the latest normalized Factiliza result per DNI for local reuse; it is not proof of ownership or a customer contract snapshot.';
COMMENT ON COLUMN identity_document_cache.id IS
'Internal numeric identifier of the cached identity result.';
COMMENT ON COLUMN identity_document_cache.document_number IS
'Eight ASCII digits stored as text to preserve leading zeros; unique across this cache.';
COMMENT ON COLUMN identity_document_cache.given_names IS
'Normalized given names returned by the identity provider; required and non-empty.';
COMMENT ON COLUMN identity_document_cache.first_surname IS
'First surname returned by the provider, when available.';
COMMENT ON COLUMN identity_document_cache.second_surname IS
'Second surname returned by the provider, when available.';
COMMENT ON COLUMN identity_document_cache.full_name IS
'Given names followed by available surnames for form completion; not silently truncated.';
COMMENT ON COLUMN identity_document_cache.provider IS
'Provider that supplied the cached result; currently restricted to FACTILIZA.';
COMMENT ON COLUMN identity_document_cache.consulted_at IS
'Instant when this result was obtained, not the date when the provider updated its underlying source.';
COMMENT ON COLUMN identity_document_cache.expires_at IS
'End of local reuse validity; expiration does not trigger physical deletion.';
COMMENT ON COLUMN identity_document_cache.created_by IS
'Internal user identifier responsible for initially storing the result; not a business relationship.';
COMMENT ON COLUMN identity_document_cache.updated_by IS
'Internal user identifier responsible for the latest accepted replacement of the result.';
COMMENT ON COLUMN identity_document_cache.created_at IS
'Creation instant preserved when the result is replaced through the native upsert.';
COMMENT ON COLUMN identity_document_cache.updated_at IS
'Update instant explicitly supplied by the native upsert when a result is replaced.';

CREATE TABLE identity_lookup_usage (
    bucket_key VARCHAR(100) PRIMARY KEY,
    request_count BIGINT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_identity_usage_count CHECK (request_count > 0)
);
CREATE INDEX idx_identity_usage_expiration ON identity_lookup_usage (expires_at);

COMMENT ON TABLE identity_lookup_usage IS
'Stores local external-call reservations by calendar month and fixed global or user minute buckets; it is not the provider account balance.';
COMMENT ON COLUMN identity_lookup_usage.bucket_key IS
'Natural primary key identifying the budget period and scope; user buckets include an internal user identifier, not a DNI.';
COMMENT ON COLUMN identity_lookup_usage.request_count IS
'Positive count of reserved external attempts, including attempts whose external outcome failed or remained uncertain.';
COMMENT ON COLUMN identity_lookup_usage.expires_at IS
'End of the bucket period for retention management; expired buckets are not deleted automatically.';