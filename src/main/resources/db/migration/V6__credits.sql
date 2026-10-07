/*=========================================================
 Module: Credits
 Migration: V6
 Description:
 Creates the persistence model for formalized financial
 credits.
=========================================================*/


/*=========================================================
 CREDIT STATUS
 Represents the financial lifecycle of a formalized credit.
=========================================================*/

CREATE TYPE credit_status AS ENUM (
    'ACTIVE',
    'IN_ARREARS',
    'PAID_OFF'
);


/*=========================================================
 CREDIT NUMBER SEQUENCE
 Provides the numeric correlation used to generate visible
 credit numbers.
=========================================================*/

CREATE SEQUENCE credit_number_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE
    CACHE 1
    NO CYCLE
    OWNED BY NONE;


/*=========================================================
 CREDITS
 Stores formal financial obligations created after the
 corresponding financing operation has been contractually
 formalized.
=========================================================*/

CREATE TABLE credits (

    /*-----------------------------------------------------
     Primary key
    -----------------------------------------------------*/

    id BIGSERIAL,

    /*-----------------------------------------------------
     Public and business identifiers
    -----------------------------------------------------*/

    credit_code UUID NOT NULL,

    credit_number VARCHAR(20) NOT NULL,

    /*-----------------------------------------------------
     Financial conditions
    -----------------------------------------------------*/

    currency VARCHAR(3) NOT NULL,

    vehicle_price NUMERIC(12,2) NOT NULL,

    down_payment NUMERIC(12,2) NOT NULL,

    financed_amount NUMERIC(12,2) NOT NULL,

    compensatory_interest_rate NUMERIC(5,2) NOT NULL,

    moratory_interest_rate NUMERIC(5,2) NOT NULL,

    installment_count INTEGER NOT NULL,

    contract_signed_at TIMESTAMPTZ NOT NULL,


    /*-----------------------------------------------------
     Relationships
    -----------------------------------------------------*/

    credit_application_id BIGINT NOT NULL,

    customer_id BIGINT NOT NULL,

    vehicle_id BIGINT NOT NULL,

    branch_id BIGINT NOT NULL,


    /*-----------------------------------------------------
     Financial lifecycle
    -----------------------------------------------------*/

    status credit_status NOT NULL DEFAULT 'ACTIVE',


    /*-----------------------------------------------------
     Audit
    -----------------------------------------------------*/

    created_by BIGINT,

    updated_by BIGINT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ,


    /*-----------------------------------------------------
     Soft delete
    -----------------------------------------------------*/

    deleted_at TIMESTAMPTZ,

    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,


    /*-----------------------------------------------------
     Primary key
    -----------------------------------------------------*/

    CONSTRAINT pk_credits
        PRIMARY KEY (id),


    /*-----------------------------------------------------
     Unique constraints
    -----------------------------------------------------*/

    CONSTRAINT uk_credits_credit_code
        UNIQUE (credit_code),

    CONSTRAINT uk_credits_credit_number
        UNIQUE (credit_number),

    CONSTRAINT uk_credits_credit_application
        UNIQUE (credit_application_id),


    /*-----------------------------------------------------
     Financial constraints
    -----------------------------------------------------*/

    CONSTRAINT chk_credits_currency
        CHECK (
            currency IN ('PEN', 'USD')
        ),

    CONSTRAINT chk_credits_vehicle_price
        CHECK (
            vehicle_price > 0
        ),

    CONSTRAINT chk_credits_down_payment
        CHECK (
            down_payment >= 0
            AND down_payment < vehicle_price
        ),

    CONSTRAINT chk_credits_financed_amount
        CHECK (
            financed_amount > 0
            AND financed_amount = vehicle_price - down_payment
        ),

    CONSTRAINT chk_credits_compensatory_interest_rate
        CHECK (
            compensatory_interest_rate >= 0
        ),

    CONSTRAINT chk_credits_moratory_interest_rate
        CHECK (
            moratory_interest_rate >= 0
        ),

    CONSTRAINT chk_credits_installment_count
        CHECK (
            installment_count BETWEEN 1 AND 12
        ),


    /*-----------------------------------------------------
     Soft delete consistency
    -----------------------------------------------------*/

    CONSTRAINT chk_credits_soft_delete
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

    CONSTRAINT fk_credits_credit_application
        FOREIGN KEY (credit_application_id)
        REFERENCES credit_applications(id),

    CONSTRAINT fk_credits_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id),

    CONSTRAINT fk_credits_vehicle
        FOREIGN KEY (vehicle_id)
        REFERENCES vehicles(id),

    CONSTRAINT fk_credits_branch
        FOREIGN KEY (branch_id)
        REFERENCES branches(id)
);


/*=========================================================
 INDEXES

 UNIQUE constraints already provide indexes for:
 - credit_code
 - credit_number
 - credit_application_id

 Therefore no additional indexes are created for them.
=========================================================*/

CREATE INDEX idx_credits_customer
    ON credits(customer_id);

CREATE INDEX idx_credits_vehicle
    ON credits(vehicle_id);

CREATE INDEX idx_credits_branch
    ON credits(branch_id);

CREATE INDEX idx_credits_status
    ON credits(status);


/*=========================================================
 DOCUMENTATION
=========================================================*/

COMMENT ON TYPE credit_status IS
'Financial lifecycle status of a formally created credit.';

COMMENT ON SEQUENCE credit_number_seq IS
'Provides the numeric correlation used to generate human-readable credit numbers.';


COMMENT ON TABLE credits IS
'Stores formal financial obligations created from approved financing applications after contractual formalization.';


COMMENT ON COLUMN credits.id IS
'Internal numeric identifier of the credit.';

COMMENT ON COLUMN credits.credit_code IS
'Public UUID used to identify the credit outside the database.';

COMMENT ON COLUMN credits.credit_number IS
'Unique human-readable business number assigned to the credit.';

COMMENT ON COLUMN credits.currency IS
'Three-character currency code of the financing operation. Initially PEN or USD.';

COMMENT ON COLUMN credits.vehicle_price IS
'Snapshotted sale price of the financed vehicle at the time the financial conditions were formalized.';

COMMENT ON COLUMN credits.down_payment IS
'Snapshotted down payment associated with the financing operation.';

COMMENT ON COLUMN credits.financed_amount IS
'Principal amount financed by the operation, calculated as vehicle price minus down payment.';

COMMENT ON COLUMN credits.compensatory_interest_rate IS
'Contractual monthly compensatory interest rate expressed as a percentage. For example, 1.50 represents 1.50 percent.';

COMMENT ON COLUMN credits.moratory_interest_rate IS
'Contractual moratory interest rate expressed as a percentage. Its application rules are defined by the corresponding collection rules.';

COMMENT ON COLUMN credits.installment_count IS
'Number of monthly installments agreed for the financing operation. Valid values range from 1 to 12.';

COMMENT ON COLUMN credits.contract_signed_at IS
'Instant when the financing contract was signed and the financial obligation was formally established.';

COMMENT ON COLUMN credits.customer_id IS
'Internal identifier of the customer responsible for the financial obligation.';

COMMENT ON COLUMN credits.vehicle_id IS
'Internal identifier of the vehicle financed by the credit.';

COMMENT ON COLUMN credits.branch_id IS
'Internal identifier of the branch responsible for the financing operation.';

COMMENT ON COLUMN credits.status IS
'Current financial lifecycle status of the credit: ACTIVE, IN_ARREARS or PAID_OFF.';

COMMENT ON COLUMN credits.created_by IS
'Identifier of the user responsible for creating the record for auditing purposes.';

COMMENT ON COLUMN credits.updated_by IS
'Identifier of the user responsible for the most recent update for auditing purposes.';

COMMENT ON COLUMN credits.created_at IS
'Timestamp when the credit record was created in the system.';

COMMENT ON COLUMN credits.updated_at IS
'Timestamp when the credit record was most recently updated.';

COMMENT ON COLUMN credits.deleted_at IS
'Timestamp when the credit was logically deleted. Null while the record remains active in normal queries.';

COMMENT ON COLUMN credits.is_deleted IS
'Indicates whether the credit has been logically deleted.';

COMMENT ON COLUMN credits.credit_application_id IS
'Internal identifier of the financing application that originated this formal credit. Every credit must reference exactly one financing application.';