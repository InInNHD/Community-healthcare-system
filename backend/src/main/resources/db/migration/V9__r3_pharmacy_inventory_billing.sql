-- V9 / R3：建立处方审方、批次库存、收费退款和模拟医保申报闭环。
-- 药品目录、仓库、批次与不可变库存流水
CREATE TABLE rx_medicine_sku (
    id BIGINT NOT NULL AUTO_INCREMENT, code VARCHAR(64) NOT NULL, name VARCHAR(255) NOT NULL,
    strength VARCHAR(64) NULL, unit VARCHAR(32) NOT NULL, active BIT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_rx_medicine_sku_code (code), KEY idx_rx_medicine_sku_name (name, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE inventory_warehouse (
    id BIGINT NOT NULL AUTO_INCREMENT, site_id BIGINT NOT NULL, code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL, active BIT NOT NULL DEFAULT 1, created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_inventory_warehouse_site_code (site_id, code),
    CONSTRAINT fk_inventory_warehouse_site FOREIGN KEY (site_id) REFERENCES site(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE inventory_batch (
    id BIGINT NOT NULL AUTO_INCREMENT, warehouse_id BIGINT NOT NULL, sku_id BIGINT NOT NULL,
    lot_number VARCHAR(64) NOT NULL, expires_on DATE NOT NULL, quantity_on_hand INT NOT NULL,
    created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_inventory_batch_lot (warehouse_id, sku_id, lot_number),
    KEY idx_inventory_batch_fefo (sku_id, expires_on, quantity_on_hand),
    CONSTRAINT fk_inventory_batch_warehouse FOREIGN KEY (warehouse_id) REFERENCES inventory_warehouse(id),
    CONSTRAINT fk_inventory_batch_sku FOREIGN KEY (sku_id) REFERENCES rx_medicine_sku(id),
    CONSTRAINT chk_inventory_batch_nonnegative CHECK (quantity_on_hand >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE inventory_transaction (
    id BIGINT NOT NULL AUTO_INCREMENT, batch_id BIGINT NOT NULL, transaction_type VARCHAR(32) NOT NULL,
    quantity INT NOT NULL, balance_after INT NOT NULL, reference_type VARCHAR(64) NOT NULL,
    reference_id VARCHAR(128) NOT NULL, actor_staff_id BIGINT NULL, occurred_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_inventory_tx_reference_batch (reference_type, reference_id, batch_id),
    KEY idx_inventory_tx_batch_time (batch_id, occurred_at),
    CONSTRAINT fk_inventory_tx_batch FOREIGN KEY (batch_id) REFERENCES inventory_batch(id),
    CONSTRAINT fk_inventory_tx_staff FOREIGN KEY (actor_staff_id) REFERENCES staff_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 处方、审方、配药核对与发药明细
CREATE TABLE rx_prescription (
    id BIGINT NOT NULL AUTO_INCREMENT, encounter_id BIGINT NOT NULL, patient_id BIGINT NOT NULL,
    prescribed_by_staff_id BIGINT NOT NULL, diagnosis VARCHAR(500) NOT NULL, status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    signed_at DATETIME(6) NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (id), KEY idx_rx_prescription_patient (patient_id, created_at),
    KEY idx_rx_prescription_queue (status, updated_at),
    CONSTRAINT fk_rx_prescription_encounter FOREIGN KEY (encounter_id) REFERENCES clinical_encounter(id),
    CONSTRAINT fk_rx_prescription_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_rx_prescription_staff FOREIGN KEY (prescribed_by_staff_id) REFERENCES staff_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE rx_prescription_item (
    id BIGINT NOT NULL AUTO_INCREMENT, prescription_id BIGINT NOT NULL, sku_id BIGINT NOT NULL,
    quantity INT NOT NULL, dosage VARCHAR(255) NOT NULL, created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id), KEY idx_rx_item_prescription (prescription_id),
    CONSTRAINT fk_rx_item_prescription FOREIGN KEY (prescription_id) REFERENCES rx_prescription(id),
    CONSTRAINT fk_rx_item_sku FOREIGN KEY (sku_id) REFERENCES rx_medicine_sku(id),
    CONSTRAINT chk_rx_item_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE rx_review (
    id BIGINT NOT NULL AUTO_INCREMENT, prescription_id BIGINT NOT NULL, pharmacist_staff_id BIGINT NOT NULL,
    decision VARCHAR(32) NOT NULL, note VARCHAR(500) NULL, reviewed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_rx_review_prescription (prescription_id),
    CONSTRAINT fk_rx_review_prescription FOREIGN KEY (prescription_id) REFERENCES rx_prescription(id),
    CONSTRAINT fk_rx_review_pharmacist FOREIGN KEY (pharmacist_staff_id) REFERENCES staff_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE rx_dispense (
    id BIGINT NOT NULL AUTO_INCREMENT, prescription_id BIGINT NOT NULL, pharmacist_staff_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL, checked_at DATETIME(6) NULL, dispensed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_rx_dispense_prescription (prescription_id),
    CONSTRAINT fk_rx_dispense_prescription FOREIGN KEY (prescription_id) REFERENCES rx_prescription(id),
    CONSTRAINT fk_rx_dispense_pharmacist FOREIGN KEY (pharmacist_staff_id) REFERENCES staff_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE rx_dispense_item (
    id BIGINT NOT NULL AUTO_INCREMENT, dispense_id BIGINT NOT NULL, prescription_item_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL, quantity INT NOT NULL, PRIMARY KEY (id),
    UNIQUE KEY uk_rx_dispense_item_batch (dispense_id, prescription_item_id, batch_id),
    CONSTRAINT fk_rx_dispense_item_dispense FOREIGN KEY (dispense_id) REFERENCES rx_dispense(id),
    CONSTRAINT fk_rx_dispense_item_rx_item FOREIGN KEY (prescription_item_id) REFERENCES rx_prescription_item(id),
    CONSTRAINT fk_rx_dispense_item_batch FOREIGN KEY (batch_id) REFERENCES inventory_batch(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 收费目录、账单、支付、退款和日结对账
CREATE TABLE billing_charge_item (
    id BIGINT NOT NULL AUTO_INCREMENT, patient_id BIGINT NOT NULL, prescription_id BIGINT NULL,
    description VARCHAR(255) NOT NULL, amount DECIMAL(12,2) NOT NULL, status VARCHAR(32) NOT NULL DEFAULT 'UNBILLED',
    created_at DATETIME(6) NOT NULL, PRIMARY KEY (id), KEY idx_charge_patient_status (patient_id, status),
    CONSTRAINT fk_charge_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_charge_prescription FOREIGN KEY (prescription_id) REFERENCES rx_prescription(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE billing_invoice (
    id BIGINT NOT NULL AUTO_INCREMENT, patient_id BIGINT NOT NULL, prescription_id BIGINT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT', total_amount DECIMAL(12,2) NOT NULL,
    paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0, refunded_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    issued_at DATETIME(6) NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (id), KEY idx_invoice_patient (patient_id, created_at),
    CONSTRAINT fk_invoice_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_invoice_prescription FOREIGN KEY (prescription_id) REFERENCES rx_prescription(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE billing_invoice_line (
    id BIGINT NOT NULL AUTO_INCREMENT, invoice_id BIGINT NOT NULL, charge_item_id BIGINT NOT NULL,
    description VARCHAR(255) NOT NULL, amount DECIMAL(12,2) NOT NULL, PRIMARY KEY (id),
    CONSTRAINT fk_invoice_line_invoice FOREIGN KEY (invoice_id) REFERENCES billing_invoice(id),
    CONSTRAINT fk_invoice_line_charge FOREIGN KEY (charge_item_id) REFERENCES billing_charge_item(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE billing_payment (
    id BIGINT NOT NULL AUTO_INCREMENT, invoice_id BIGINT NOT NULL, amount DECIMAL(12,2) NOT NULL,
    channel VARCHAR(32) NOT NULL, external_reference VARCHAR(128) NULL, paid_at DATETIME(6) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL, PRIMARY KEY (id), UNIQUE KEY uk_payment_idempotency (idempotency_key),
    KEY idx_payment_invoice (invoice_id, paid_at), CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id) REFERENCES billing_invoice(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE billing_refund (
    id BIGINT NOT NULL AUTO_INCREMENT, invoice_id BIGINT NOT NULL, amount DECIMAL(12,2) NOT NULL,
    reason VARCHAR(500) NOT NULL, refunded_by_staff_id BIGINT NOT NULL, refunded_at DATETIME(6) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL, PRIMARY KEY (id), UNIQUE KEY uk_refund_idempotency (idempotency_key),
    KEY idx_refund_invoice (invoice_id, refunded_at), CONSTRAINT fk_refund_invoice FOREIGN KEY (invoice_id) REFERENCES billing_invoice(id),
    CONSTRAINT fk_refund_staff FOREIGN KEY (refunded_by_staff_id) REFERENCES staff_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE billing_daily_settlement (
    id BIGINT NOT NULL AUTO_INCREMENT, site_id BIGINT NOT NULL, business_date DATE NOT NULL,
    payment_total DECIMAL(12,2) NOT NULL, refund_total DECIMAL(12,2) NOT NULL,
    status VARCHAR(32) NOT NULL, settled_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_daily_settlement (site_id, business_date),
    CONSTRAINT fk_daily_settlement_site FOREIGN KEY (site_id) REFERENCES site(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 模拟医保申报及其状态事件；真实接口由适配器替换
CREATE TABLE insurance_claim (
    id BIGINT NOT NULL AUTO_INCREMENT, invoice_id BIGINT NOT NULL, patient_id BIGINT NOT NULL,
    claimed_amount DECIMAL(12,2) NOT NULL, settled_amount DECIMAL(12,2) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT', simulation BIT NOT NULL DEFAULT 1,
    external_reference VARCHAR(128) NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (id), KEY idx_insurance_claim_invoice (invoice_id, status),
    CONSTRAINT fk_insurance_claim_invoice FOREIGN KEY (invoice_id) REFERENCES billing_invoice(id),
    CONSTRAINT fk_insurance_claim_patient FOREIGN KEY (patient_id) REFERENCES patient(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE insurance_claim_event (
    id BIGINT NOT NULL AUTO_INCREMENT, claim_id BIGINT NOT NULL, event_type VARCHAR(32) NOT NULL,
    payload_json TEXT NULL, occurred_at DATETIME(6) NOT NULL, idempotency_key VARCHAR(128) NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_insurance_event_idempotency (idempotency_key),
    KEY idx_insurance_event_claim (claim_id, occurred_at),
    CONSTRAINT fk_insurance_event_claim FOREIGN KEY (claim_id) REFERENCES insurance_claim(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
