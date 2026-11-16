/*=========================================================
 Module: Customers
 Migration: V3
 Description:
 Creates the persistence model for customers who may
 participate in commercial and financing operations.
=========================================================*/


/*=========================================================
 CUSTOMERS

 Stores customers registered in the system.

 Each customer is identified by an accepted identity
 document and belongs to a branch.

 Customer records preserve their historical information
 even when they become inactive or are logically deleted.
=========================================================*/

CREATE TABLE customers (

    /*-----------------------------------------------------
     Primary key
    -----------------------------------------------------*/

    id BIGSERIAL,


    /*-----------------------------------------------------
     Public identifier
    -----------------------------------------------------*/

    customer_code UUID NOT NULL,


    /*-----------------------------------------------------
     Identity information
    -----------------------------------------------------*/

    full_name VARCHAR(150) NOT NULL,

    document_type VARCHAR(10) NOT NULL,

    document_number VARCHAR(20) NOT NULL,


    /*-----------------------------------------------------
     Contact information
    -----------------------------------------------------*/

    phone VARCHAR(20),

    email VARCHAR(120),


    /*-----------------------------------------------------
     Economic information
    -----------------------------------------------------*/

    occupation VARCHAR(150),

    monthly_income NUMERIC(12,2),


    /*-----------------------------------------------------
     Residence information
    -----------------------------------------------------*/

    address VARCHAR(255),

    map_reference VARCHAR(255),


    /*-----------------------------------------------------
     Relationships
    -----------------------------------------------------*/

    branch_id BIGINT NOT NULL,


    /*-----------------------------------------------------
     Operational status
    -----------------------------------------------------*/

    is_active BOOLEAN NOT NULL DEFAULT TRUE,


    /*-----------------------------------------------------
     Audit
    -----------------------------------------------------*/

    created_by BIGINT,

    updated_by BIGINT,

    created_at TIMESTAMPTZ
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ,


    /*-----------------------------------------------------
     Soft delete
    -----------------------------------------------------*/

    deleted_at TIMESTAMPTZ,

    is_deleted BOOLEAN
        NOT NULL
        DEFAULT FALSE,


    /*-----------------------------------------------------
     Primary key
    -----------------------------------------------------*/

    CONSTRAINT pk_customers
        PRIMARY KEY (id),


    /*-----------------------------------------------------
     Unique constraints
    -----------------------------------------------------*/

    CONSTRAINT uk_customers_customer_code
        UNIQUE (customer_code),

    CONSTRAINT uk_customers_document
        UNIQUE (
            document_type,
            document_number
        ),


    /*-----------------------------------------------------
     Identity consistency
    -----------------------------------------------------*/

    CONSTRAINT chk_customers_full_name_not_empty
        CHECK (
            BTRIM(full_name) <> ''
        ),

    CONSTRAINT chk_customers_document_type
        CHECK (
            document_type IN (
                'DNI',
                'CE'
            )
        ),

    CONSTRAINT chk_customers_document_number_not_empty
        CHECK (
            BTRIM(document_number) <> ''
        ),


    /*-----------------------------------------------------
     Economic consistency
    -----------------------------------------------------*/

    CONSTRAINT chk_customers_monthly_income
        CHECK (
            monthly_income IS NULL
            OR monthly_income >= 0
        ),


    /*-----------------------------------------------------
     Soft delete consistency
    -----------------------------------------------------*/

    CONSTRAINT chk_customers_soft_delete
        CHECK (
            (
                is_deleted = FALSE
                AND deleted_at IS NULL
            )
            OR
            (
                is_deleted = TRUE
                AND deleted_at IS NOT NULL
            )
        ),


    /*-----------------------------------------------------
     Foreign keys
    -----------------------------------------------------*/

    CONSTRAINT fk_customers_branch
        FOREIGN KEY (branch_id)
        REFERENCES branches(id)
);


/*=========================================================
 INDEXES

 UNIQUE constraints already provide indexes for:
 - customer_code
 - document_type + document_number

 Therefore no additional indexes are created for them.
=========================================================*/

CREATE INDEX idx_customers_branch
    ON customers(branch_id);

CREATE INDEX idx_customers_branch_active
    ON customers(branch_id, is_active)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX uk_customers_ce_document_upper
    ON customers (upper(document_number))
    WHERE document_type = 'CE';


/*=========================================================
 DOCUMENTATION
=========================================================*/

COMMENT ON TABLE customers IS
'Stores customers registered for commercial and financing operations.';


COMMENT ON COLUMN customers.id IS
'Internal numeric identifier of the customer.';

COMMENT ON COLUMN customers.customer_code IS
'Public UUID used to identify the customer outside the database.';

COMMENT ON COLUMN customers.full_name IS
'Full legal or commonly registered name of the customer.';

COMMENT ON COLUMN customers.document_type IS
'Type of identity document presented by the customer. Initially DNI or CE.';

COMMENT ON COLUMN customers.document_number IS
'Identity document number associated with the selected document type.';

COMMENT ON COLUMN customers.phone IS
'Primary mobile or contact phone number of the customer.';

COMMENT ON COLUMN customers.email IS
'Contact email address of the customer.';

COMMENT ON COLUMN customers.occupation IS
'Occupation or principal economic activity reported by the customer.';

COMMENT ON COLUMN customers.monthly_income IS
'Net monthly income reported by the customer for financing evaluation purposes.';

COMMENT ON COLUMN customers.address IS
'Primary residential address reported by the customer.';

COMMENT ON COLUMN customers.map_reference IS
'Additional geographic or descriptive reference used to locate the customer residence.';

COMMENT ON COLUMN customers.branch_id IS
'Internal identifier of the branch responsible for the customer record.';

COMMENT ON COLUMN customers.is_active IS
'Indicates whether the customer is currently available for new business operations.';

COMMENT ON COLUMN customers.created_by IS
'Identifier of the user responsible for creating the customer record for auditing purposes.';

COMMENT ON COLUMN customers.updated_by IS
'Identifier of the user responsible for the most recent update for auditing purposes.';

COMMENT ON COLUMN customers.created_at IS
'Timestamp when the customer record was created in the system.';

COMMENT ON COLUMN customers.updated_at IS
'Timestamp when the customer record was most recently updated.';

COMMENT ON COLUMN customers.deleted_at IS
'Timestamp when the customer was logically deleted. Null while the record remains active in normal queries.';

COMMENT ON COLUMN customers.is_deleted IS
'Indicates whether the customer has been logically deleted.';