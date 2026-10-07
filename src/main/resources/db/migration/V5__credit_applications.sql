/*=========================================================
 Module: Credits
 Migration: V5
 Description:
 Creates the persistence model for financing applications
 submitted before the creation of a formal financial
 obligation.
=========================================================*/


/*=========================================================
 CREDIT APPLICATION STATUS
 Represents the lifecycle of a financing application before
 the contractual financial obligation is created.
=========================================================*/

CREATE TYPE credit_application_status AS ENUM (
    'PENDING',
    'APPROVED',
    'REJECTED',
    'CANCELLED',
    'EXPIRED'
);


/*=========================================================
 CREDIT APPLICATION NUMBER SEQUENCE
 Provides the numeric correlation used to generate visible
 financing application numbers.
=========================================================*/

CREATE SEQUENCE credit_application_number_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE
    CACHE 1
    NO CYCLE
    OWNED BY NONE;


/*=========================================================
 CREDIT APPLICATIONS
 Stores financing applications submitted for evaluation
 before the existence of a formal financial obligation.
=========================================================*/

CREATE TABLE credit_applications (

    /*-----------------------------------------------------
     Primary key
    -----------------------------------------------------*/

    id BIGSERIAL,


    /*-----------------------------------------------------
     Public and business identifiers
    -----------------------------------------------------*/

    application_code UUID NOT NULL,

    application_number VARCHAR(20) NOT NULL,


    /*-----------------------------------------------------
     Financial conditions snapshot
    -----------------------------------------------------*/

    currency VARCHAR(3) NOT NULL,

    vehicle_price NUMERIC(12,2) NOT NULL,

    down_payment NUMERIC(12,2) NOT NULL,

    financed_amount NUMERIC(12,2) NOT NULL,

    compensatory_interest_rate NUMERIC(5,2) NOT NULL,

    installment_count INTEGER NOT NULL,

    /*-----------------------------------------------------
     Relationships
    -----------------------------------------------------*/

    customer_id BIGINT NOT NULL,

    vehicle_id BIGINT NOT NULL,

    branch_id BIGINT NOT NULL,


    /*-----------------------------------------------------
     Application lifecycle and resolution
    -----------------------------------------------------*/

    status credit_application_status
        NOT NULL
        DEFAULT 'PENDING',

    resolved_at TIMESTAMPTZ,

    resolved_by BIGINT,

    rejection_reason TEXT,


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

    CONSTRAINT pk_credit_applications
        PRIMARY KEY (id),


    /*-----------------------------------------------------
     Unique constraints
    -----------------------------------------------------*/

    CONSTRAINT uk_credit_applications_application_code
        UNIQUE (application_code),

    CONSTRAINT uk_credit_applications_application_number
        UNIQUE (application_number),


    /*-----------------------------------------------------
     Financial constraints
    -----------------------------------------------------*/

    CONSTRAINT chk_credit_applications_currency
        CHECK (
            currency IN ('PEN', 'USD')
        ),

    CONSTRAINT chk_credit_applications_vehicle_price
        CHECK (
            vehicle_price > 0
        ),

    CONSTRAINT chk_credit_applications_down_payment
        CHECK (
            down_payment >= 0
            AND down_payment < vehicle_price
        ),

    CONSTRAINT chk_credit_applications_financed_amount
        CHECK (
            financed_amount > 0
            AND financed_amount = vehicle_price - down_payment
        ),

    CONSTRAINT chk_credit_applications_compensatory_interest_rate
        CHECK (
            compensatory_interest_rate >= 0
        ),

    CONSTRAINT chk_credit_applications_installment_count
        CHECK (
            installment_count BETWEEN 1 AND 12
        ),


    /*-----------------------------------------------------
     Resolution consistency

     PENDING applications have not been resolved.

     APPROVED applications must preserve both the resolution
     instant and the responsible user.

     REJECTED, CANCELLED and EXPIRED applications must preserve
     the resolution instant, while resolved_by may remain null
     for future system-driven resolutions.
    -----------------------------------------------------*/

    CONSTRAINT chk_credit_applications_resolution
        CHECK (
            (
                status = 'PENDING'
                AND resolved_at IS NULL
                AND resolved_by IS NULL
            )
            OR
            (
                status = 'APPROVED'
                AND resolved_at IS NOT NULL
                AND resolved_by IS NOT NULL
            )
            OR
            (
                status IN (
                    'REJECTED',
                    'CANCELLED',
                    'EXPIRED'
                )
                AND resolved_at IS NOT NULL
            )
        ),


    /*-----------------------------------------------------
     Rejection consistency

     A rejection must preserve a meaningful reason.
     Other application states must not contain a rejection
     reason.
    -----------------------------------------------------*/

    CONSTRAINT chk_credit_applications_rejection_reason
        CHECK (
            (
                status = 'REJECTED'
                AND rejection_reason IS NOT NULL
                AND BTRIM(rejection_reason) <> ''
            )
            OR
            (
                status <> 'REJECTED'
                AND rejection_reason IS NULL
            )
        ),


    /*-----------------------------------------------------
     Soft delete consistency
    -----------------------------------------------------*/

    CONSTRAINT chk_credit_applications_soft_delete
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

    CONSTRAINT fk_credit_applications_customer
        FOREIGN KEY (customer_id)
        REFERENCES customers(id),

    CONSTRAINT fk_credit_applications_vehicle
        FOREIGN KEY (vehicle_id)
        REFERENCES vehicles(id),

    CONSTRAINT fk_credit_applications_branch
        FOREIGN KEY (branch_id)
        REFERENCES branches(id)
);


/*=========================================================
 INDEXES

 UNIQUE constraints already provide indexes for:
 - application_code
 - application_number

 Therefore no additional indexes are created for them.
=========================================================*/

CREATE INDEX idx_credit_applications_customer
    ON credit_applications(customer_id);

CREATE INDEX idx_credit_applications_vehicle
    ON credit_applications(vehicle_id);

CREATE INDEX idx_credit_applications_branch
    ON credit_applications(branch_id);

CREATE INDEX idx_credit_applications_status
    ON credit_applications(status);


/*=========================================================
 DOCUMENTATION
=========================================================*/

COMMENT ON TYPE credit_application_status IS
'Lifecycle status of a financing application before the creation of a formal financial obligation.';


COMMENT ON SEQUENCE credit_application_number_seq IS
'Provides the numeric correlation used to generate human-readable financing application numbers.';


COMMENT ON TABLE credit_applications IS
'Stores financing applications submitted by customers for evaluation before a formal financial obligation is created.';


COMMENT ON COLUMN credit_applications.id IS
'Internal numeric identifier of the financing application.';

COMMENT ON COLUMN credit_applications.application_code IS
'Public UUID used to identify the financing application outside the database.';

COMMENT ON COLUMN credit_applications.application_number IS
'Unique human-readable business number assigned to the financing application.';

COMMENT ON COLUMN credit_applications.currency IS
'Three-character currency code selected for the financing operation. Initially PEN or USD.';

COMMENT ON COLUMN credit_applications.vehicle_price IS
'Snapshotted sale price of the vehicle used when the financing application was registered.';

COMMENT ON COLUMN credit_applications.down_payment IS
'Down payment declared for the financing application.';

COMMENT ON COLUMN credit_applications.financed_amount IS
'Amount requested for financing, calculated as vehicle price minus down payment.';

COMMENT ON COLUMN credit_applications.compensatory_interest_rate IS
'Monthly compensatory interest rate fixed when the financing application is registered, expressed as a percentage. For example, 1.50 represents 1.50 percent.';

COMMENT ON COLUMN credit_applications.installment_count IS
'Number of monthly installments requested for the financing operation. Valid values range from 1 to 12.';

COMMENT ON COLUMN credit_applications.customer_id IS
'Internal identifier of the customer submitting the financing application.';

COMMENT ON COLUMN credit_applications.vehicle_id IS
'Internal identifier of the vehicle associated with the financing application.';

COMMENT ON COLUMN credit_applications.branch_id IS
'Internal identifier of the branch where the financing application was registered.';

COMMENT ON COLUMN credit_applications.status IS
'Current lifecycle status of the financing application: PENDING, APPROVED, REJECTED, CANCELLED or EXPIRED.';

COMMENT ON COLUMN credit_applications.resolved_at IS
'Timestamp when the financing application left PENDING and obtained a terminal resolution. Null while the application remains pending.';

COMMENT ON COLUMN credit_applications.resolved_by IS
'Identifier of the user responsible for resolving the financing application when the resolution is performed by a user. May be null for system-driven resolutions.';

COMMENT ON COLUMN credit_applications.rejection_reason IS
'Reason explaining why the financing application was rejected. Required only when status is REJECTED.';

COMMENT ON COLUMN credit_applications.created_by IS
'Identifier of the user responsible for creating the record for auditing purposes.';

COMMENT ON COLUMN credit_applications.updated_by IS
'Identifier of the user responsible for the most recent update for auditing purposes.';

COMMENT ON COLUMN credit_applications.created_at IS
'Timestamp when the financing application record was created in the system.';

COMMENT ON COLUMN credit_applications.updated_at IS
'Timestamp when the financing application record was most recently updated.';

COMMENT ON COLUMN credit_applications.deleted_at IS
'Timestamp when the financing application was logically deleted. Null while the record remains active in normal queries.';

COMMENT ON COLUMN credit_applications.is_deleted IS
'Indicates whether the financing application has been logically deleted.';
