-- V4：统一旧版临床数据的逻辑删除标记，避免删除历史医疗事实。
ALTER TABLE appointment
    ADD COLUMN active BIT NOT NULL DEFAULT 1;

ALTER TABLE health_record
    ADD COLUMN active BIT NOT NULL DEFAULT 1;

ALTER TABLE patient
    ADD COLUMN deleted_at DATETIME(6) NULL,
    ADD COLUMN deleted_by VARCHAR(128) NULL;

ALTER TABLE doctor
    ADD COLUMN deleted_at DATETIME(6) NULL,
    ADD COLUMN deleted_by VARCHAR(128) NULL;

ALTER TABLE appointment
    ADD COLUMN deleted_at DATETIME(6) NULL,
    ADD COLUMN deleted_by VARCHAR(128) NULL;

ALTER TABLE health_record
    ADD COLUMN deleted_at DATETIME(6) NULL,
    ADD COLUMN deleted_by VARCHAR(128) NULL;

ALTER TABLE medicine
    ADD COLUMN deleted_at DATETIME(6) NULL,
    ADD COLUMN deleted_by VARCHAR(128) NULL;

ALTER TABLE chronic_case
    ADD COLUMN deleted_at DATETIME(6) NULL,
    ADD COLUMN deleted_by VARCHAR(128) NULL;

CREATE INDEX idx_appointment_active ON appointment (active);
CREATE INDEX idx_health_record_active ON health_record (active);
