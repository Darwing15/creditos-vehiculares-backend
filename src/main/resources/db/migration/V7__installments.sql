/*=========================================================
 Module: Credits
 Migration: V7
 Description:
 Creates the persistence model for contractual installments
 generated as part of a formalized credit payment schedule.
=========================================================*/


/*=========================================================
 INSTALLMENTS

 Stores the contractual installments generated for a
 formalized Credit.

 Each installment preserves the financial composition and
 contractual due date established when the payment schedule
 is generated.

 Payment status and overdue condition are derived from the
 installment balance and due date and are therefore not
 persisted as independent status columns.
=========================================================*/

CREATE TABLE installments (

    /*-----------------------------------------------------
     Primary key
    -----------------------------------------------------*/

    id BIGSERIAL,


    /*-----------------------------------------------------
     Contractual schedule information
    -----------------------------------------------------*/

    installment_number INTEGER NOT NULL,

    due_date DATE NOT NULL,

    principal_amount NUMERIC(12,2) NOT NULL,

    compensatory_interest_amount NUMERIC(12,2) NOT NULL,

    amount NUMERIC(12,2) NOT NULL,

    outstanding_principal NUMERIC(12,2) NOT NULL,

    balance NUMERIC(12,2) NOT NULL,


    /*-----------------------------------------------------
     Relationships
    -----------------------------------------------------*/

    credit_id BIGINT NOT NULL,


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

    CONSTRAINT pk_installments
        PRIMARY KEY (id),


    /*-----------------------------------------------------
     Unique constraints

     An installment number is unique only within its Credit.
    -----------------------------------------------------*/

    CONSTRAINT uk_installments_credit_number
        UNIQUE (
            credit_id,
            installment_number
        ),


    /*-----------------------------------------------------
     Schedule consistency
    -----------------------------------------------------*/

    CONSTRAINT chk_installments_installment_number
        CHECK (
            installment_number BETWEEN 1 AND 12
        ),

    CONSTRAINT chk_installments_principal_amount
        CHECK (
            principal_amount > 0
        ),

    CONSTRAINT chk_installments_compensatory_interest_amount
        CHECK (
            compensatory_interest_amount >= 0
        ),

    CONSTRAINT chk_installments_amount
        CHECK (
            amount > 0
            AND amount =
                principal_amount
                + compensatory_interest_amount
        ),

    CONSTRAINT chk_installments_outstanding_principal
        CHECK (
            outstanding_principal >= 0
        ),

    CONSTRAINT chk_installments_balance
        CHECK (
            balance >= 0
            AND balance <= amount
        ),


    /*-----------------------------------------------------
     Soft delete consistency
    -----------------------------------------------------*/

    CONSTRAINT chk_installments_soft_delete
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

    CONSTRAINT fk_installments_credit
        FOREIGN KEY (credit_id)
        REFERENCES credits(id)
);


/*=========================================================
 INDEXES

 The UNIQUE constraint on:

     credit_id + installment_number

 already provides an index whose leading column is credit_id.

 Therefore no separate index on credit_id is required.

 The partial due-date index supports collection queries that
 search installments which still have an outstanding balance.
=========================================================*/

CREATE INDEX idx_installments_due_date_pending
    ON installments(due_date)
    WHERE
        is_deleted = FALSE
        AND balance > 0;


/*=========================================================
 DOCUMENTATION
=========================================================*/

COMMENT ON TABLE installments IS
'Stores contractual installments generated as part of the payment schedule of a formalized credit.';


COMMENT ON COLUMN installments.id IS
'Internal numeric identifier of the installment.';

COMMENT ON COLUMN installments.installment_number IS
'Sequential installment number within the associated credit. Valid values range from 1 to 12 and must be unique within the credit.';

COMMENT ON COLUMN installments.due_date IS
'Contractual calendar date on which the installment becomes due.';

COMMENT ON COLUMN installments.principal_amount IS
'Portion of the financed principal contractually assigned to this installment.';

COMMENT ON COLUMN installments.compensatory_interest_amount IS
'Contractual compensatory interest amount assigned to this installment according to the financing conditions fixed for the credit.';

COMMENT ON COLUMN installments.amount IS
'Total contractual installment amount, composed of principal amount plus compensatory interest amount.';

COMMENT ON COLUMN installments.outstanding_principal IS
'Remaining contractual principal of the credit after the scheduled principal amortization represented by this installment.';

COMMENT ON COLUMN installments.balance IS
'Outstanding amount of this installment that remains unpaid. It excludes moratory interest and other collection concepts calculated separately.';

COMMENT ON COLUMN installments.credit_id IS
'Internal identifier of the formalized credit to which the installment belongs.';

COMMENT ON COLUMN installments.created_by IS
'Identifier of the user responsible for creating the installment record for auditing purposes.';

COMMENT ON COLUMN installments.updated_by IS
'Identifier of the user responsible for the most recent update for auditing purposes.';

COMMENT ON COLUMN installments.created_at IS
'Timestamp when the installment record was created in the system.';

COMMENT ON COLUMN installments.updated_at IS
'Timestamp when the installment record was most recently updated.';

COMMENT ON COLUMN installments.deleted_at IS
'Timestamp when the installment was logically deleted. Null while the record remains available to normal queries.';

COMMENT ON COLUMN installments.is_deleted IS
'Indicates whether the installment has been logically deleted.';