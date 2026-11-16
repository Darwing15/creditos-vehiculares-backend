/*=========================================================
 Module: Vehicles
 Migration: V4
 Description:
 Creates the persistence model for individual physical
 vehicle units managed by the inventory module.
=========================================================*/


/*=========================================================
 VEHICLE STATUS

 Represents the operational inventory status of an
 individual physical vehicle unit.

 AVAILABLE
 The vehicle may participate in a new financing operation.

 RESERVED
 The vehicle is associated with a financing application
 currently in progress.

 SOLD
 The vehicle is already associated with a formally created
 financial obligation and is no longer available for new
 operations.

 SOLD represents an inventory/commercial status and does
 not by itself represent legal transfer of ownership.
=========================================================*/

CREATE TYPE vehicle_status AS ENUM (
    'AVAILABLE',
    'RESERVED',
    'SOLD'
);


/*=========================================================
 VEHICLES

 Stores individual physical vehicle units registered in
 the company inventory.

 Each row represents one identifiable vehicle unit rather
 than an aggregated stock quantity.
=========================================================*/

CREATE TABLE vehicles (

    /*-----------------------------------------------------
     Primary key
    -----------------------------------------------------*/

    id BIGSERIAL,


    /*-----------------------------------------------------
     Public identifier
    -----------------------------------------------------*/

    vehicle_code UUID NOT NULL,


    /*-----------------------------------------------------
     General information
    -----------------------------------------------------*/

    vehicle_type VARCHAR(50) NOT NULL,

    brand VARCHAR(100) NOT NULL,

    model VARCHAR(150) NOT NULL,

    manufacturing_year INTEGER NOT NULL,

    color VARCHAR(50) NOT NULL,


    /*-----------------------------------------------------
     Physical and legal identifiers
    -----------------------------------------------------*/

    chassis_number VARCHAR(100) NOT NULL,

    engine_number VARCHAR(100) NOT NULL,

    dua_number VARCHAR(50),


    /*-----------------------------------------------------
     Commercial information
    -----------------------------------------------------*/

    reference_price NUMERIC(12,2) NOT NULL,


    /*-----------------------------------------------------
     Relationships
    -----------------------------------------------------*/

    branch_id BIGINT NOT NULL,


    /*-----------------------------------------------------
     Operational status
    -----------------------------------------------------*/

    status vehicle_status
        NOT NULL
        DEFAULT 'AVAILABLE',


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

    CONSTRAINT pk_vehicles
        PRIMARY KEY (id),


    /*-----------------------------------------------------
     Unique constraints
    -----------------------------------------------------*/

    CONSTRAINT uk_vehicles_vehicle_code
        UNIQUE (vehicle_code),

    CONSTRAINT uk_vehicles_chassis_number
        UNIQUE (chassis_number),

    CONSTRAINT uk_vehicles_engine_number
        UNIQUE (engine_number),


    /*-----------------------------------------------------
     Data consistency
    -----------------------------------------------------*/

    CONSTRAINT chk_vehicles_vehicle_type_not_empty
        CHECK (
            BTRIM(vehicle_type) <> ''
        ),

    CONSTRAINT chk_vehicles_brand_not_empty
        CHECK (
            BTRIM(brand) <> ''
        ),

    CONSTRAINT chk_vehicles_model_not_empty
        CHECK (
            BTRIM(model) <> ''
        ),

    CONSTRAINT chk_vehicles_color_not_empty
        CHECK (
            BTRIM(color) <> ''
        ),

    CONSTRAINT chk_vehicles_chassis_number_not_empty
        CHECK (
            BTRIM(chassis_number) <> ''
        ),

    CONSTRAINT chk_vehicles_engine_number_not_empty
        CHECK (
            BTRIM(engine_number) <> ''
        ),

    CONSTRAINT chk_vehicles_dua_number_not_empty
        CHECK (
            dua_number IS NULL
            OR BTRIM(dua_number) <> ''
        ),

    CONSTRAINT chk_vehicles_reference_price
        CHECK (
            reference_price > 0
        ),


    /*-----------------------------------------------------
     Soft delete consistency
    -----------------------------------------------------*/

    CONSTRAINT chk_vehicles_soft_delete
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

    CONSTRAINT fk_vehicles_branch
        FOREIGN KEY (branch_id)
        REFERENCES branches(id)
);


/*=========================================================
 INDEXES

 UNIQUE constraints already provide indexes for:
 - vehicle_code
 - chassis_number
 - engine_number

 Therefore no additional indexes are created for them.
=========================================================*/

CREATE INDEX idx_vehicles_branch_status
    ON vehicles(branch_id, status)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_vehicles_brand
    ON vehicles(brand)
    WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX uk_vehicles_chassis_number_upper
    ON vehicles (upper(chassis_number));

CREATE UNIQUE INDEX uk_vehicles_engine_number_upper
    ON vehicles (upper(engine_number));


/*=========================================================
 DOCUMENTATION
=========================================================*/

COMMENT ON TYPE vehicle_status IS
'Operational inventory status of an individual vehicle unit: AVAILABLE, RESERVED or SOLD.';


COMMENT ON TABLE vehicles IS
'Stores individual physical vehicle units registered in the company inventory.';


COMMENT ON COLUMN vehicles.id IS
'Internal numeric identifier of the vehicle unit.';

COMMENT ON COLUMN vehicles.vehicle_code IS
'Public UUID used to identify the vehicle outside the database.';

COMMENT ON COLUMN vehicles.vehicle_type IS
'Commercial category or type assigned to the vehicle, such as motorcycle or cargo tricycle.';

COMMENT ON COLUMN vehicles.brand IS
'Manufacturer or commercial brand of the vehicle.';

COMMENT ON COLUMN vehicles.model IS
'Specific commercial model of the vehicle unit.';

COMMENT ON COLUMN vehicles.manufacturing_year IS
'Manufacturing year reported for the vehicle unit.';

COMMENT ON COLUMN vehicles.color IS
'Registered color or color combination of the vehicle.';

COMMENT ON COLUMN vehicles.chassis_number IS
'Unique chassis, serial or VIN identifier of the physical vehicle unit.';

COMMENT ON COLUMN vehicles.engine_number IS
'Unique identifier engraved or assigned to the vehicle engine.';

COMMENT ON COLUMN vehicles.dua_number IS
'DUA or customs declaration reference associated with the vehicle when applicable.';

COMMENT ON COLUMN vehicles.reference_price IS
'Reference or suggested sale price configured for the vehicle inventory. The final financing price is fixed separately when the financing application is registered.';

COMMENT ON COLUMN vehicles.branch_id IS
'Internal identifier of the branch currently responsible for the vehicle unit.';

COMMENT ON COLUMN vehicles.status IS
'Current inventory status of the vehicle. AVAILABLE allows a new financing application, RESERVED represents a vehicle associated with an application in progress, and SOLD represents a vehicle committed to a formalized financial obligation. SOLD does not by itself represent legal transfer of ownership.';

COMMENT ON COLUMN vehicles.created_by IS
'Identifier of the user responsible for creating the vehicle record for auditing purposes.';

COMMENT ON COLUMN vehicles.updated_by IS
'Identifier of the user responsible for the most recent update for auditing purposes.';

COMMENT ON COLUMN vehicles.created_at IS
'Timestamp when the vehicle record was created in the system.';

COMMENT ON COLUMN vehicles.updated_at IS
'Timestamp when the vehicle record was most recently updated.';

COMMENT ON COLUMN vehicles.deleted_at IS
'Timestamp when the vehicle was logically deleted. Null while the record remains available to normal queries.';

COMMENT ON COLUMN vehicles.is_deleted IS
'Indicates whether the vehicle has been logically deleted.';