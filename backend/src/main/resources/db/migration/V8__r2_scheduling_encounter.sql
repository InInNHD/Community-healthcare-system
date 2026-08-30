-- V8 / R2：建立排班、号源、预约、签到、候诊、接诊和不可变病历版本闭环。
-- 排班与可并发抢占的号源
CREATE TABLE sched_session (
    id BIGINT NOT NULL AUTO_INCREMENT,
    site_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    staff_profile_id BIGINT NOT NULL,
    service_date DATE NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sched_session_staff_start (staff_profile_id, starts_at),
    KEY idx_sched_session_site_date (site_id, service_date, status),
    CONSTRAINT fk_sched_session_site FOREIGN KEY (site_id) REFERENCES site(id),
    CONSTRAINT fk_sched_session_department FOREIGN KEY (department_id) REFERENCES department(id),
    CONSTRAINT fk_sched_session_staff FOREIGN KEY (staff_profile_id) REFERENCES staff_profile(id),
    CONSTRAINT chk_sched_session_time CHECK (ends_at > starts_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sched_slot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sched_slot_session_start (session_id, starts_at),
    KEY idx_sched_slot_availability (status, starts_at),
    CONSTRAINT fk_sched_slot_session FOREIGN KEY (session_id) REFERENCES sched_session(id),
    CONSTRAINT chk_sched_slot_time CHECK (ends_at > starts_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 预约、签到、候诊队列及状态事件
CREATE TABLE sched_appointment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slot_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED',
    reason VARCHAR(500) NULL,
    cancelled_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sched_appointment_slot (slot_id),
    KEY idx_sched_appointment_patient (patient_id, created_at),
    KEY idx_sched_appointment_status (status, updated_at),
    CONSTRAINT fk_sched_appointment_slot FOREIGN KEY (slot_id) REFERENCES sched_slot(id),
    CONSTRAINT fk_sched_appointment_patient FOREIGN KEY (patient_id) REFERENCES patient(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sched_check_in (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT NOT NULL,
    checked_in_by_staff_id BIGINT NOT NULL,
    checked_in_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sched_check_in_appointment (appointment_id),
    CONSTRAINT fk_sched_check_in_appointment FOREIGN KEY (appointment_id) REFERENCES sched_appointment(id),
    CONSTRAINT fk_sched_check_in_staff FOREIGN KEY (checked_in_by_staff_id) REFERENCES staff_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sched_queue_entry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    queue_number INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'WAITING',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sched_queue_appointment (appointment_id),
    UNIQUE KEY uk_sched_queue_number (session_id, queue_number),
    CONSTRAINT fk_sched_queue_appointment FOREIGN KEY (appointment_id) REFERENCES sched_appointment(id),
    CONSTRAINT fk_sched_queue_session FOREIGN KEY (session_id) REFERENCES sched_session(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sched_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor VARCHAR(128) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    details_json TEXT NULL,
    PRIMARY KEY (id),
    KEY idx_sched_event_appointment (appointment_id, occurred_at),
    CONSTRAINT fk_sched_event_appointment FOREIGN KEY (appointment_id) REFERENCES sched_appointment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 线下接诊、SOAP 正文、结构化诊断和签署文档版本
CREATE TABLE clinical_encounter (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    staff_profile_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    started_at DATETIME(6) NOT NULL,
    signed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_clinical_encounter_appointment (appointment_id),
    KEY idx_clinical_encounter_patient (patient_id, started_at),
    KEY idx_clinical_encounter_staff (staff_profile_id, started_at),
    CONSTRAINT fk_clinical_encounter_appointment FOREIGN KEY (appointment_id) REFERENCES sched_appointment(id),
    CONSTRAINT fk_clinical_encounter_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_clinical_encounter_staff FOREIGN KEY (staff_profile_id) REFERENCES staff_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE clinical_note (
    id BIGINT NOT NULL AUTO_INCREMENT,
    encounter_id BIGINT NOT NULL,
    note_type VARCHAR(32) NOT NULL DEFAULT 'SOAP',
    body TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_clinical_note_encounter_type (encounter_id, note_type),
    CONSTRAINT fk_clinical_note_encounter FOREIGN KEY (encounter_id) REFERENCES clinical_encounter(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE clinical_diagnosis (
    id BIGINT NOT NULL AUTO_INCREMENT,
    encounter_id BIGINT NOT NULL,
    diagnosis_code VARCHAR(64) NOT NULL,
    diagnosis_name VARCHAR(255) NOT NULL,
    diagnosis_type VARCHAR(32) NOT NULL DEFAULT 'PRIMARY',
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_clinical_diagnosis_code (encounter_id, diagnosis_code, diagnosis_type),
    CONSTRAINT fk_clinical_diagnosis_encounter FOREIGN KEY (encounter_id) REFERENCES clinical_encounter(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE clinical_document_version (
    id BIGINT NOT NULL AUTO_INCREMENT,
    encounter_id BIGINT NOT NULL,
    document_type VARCHAR(32) NOT NULL,
    version_no INT NOT NULL,
    content TEXT NOT NULL,
    content_hash CHAR(64) NOT NULL,
    created_by_staff_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_clinical_document_version (encounter_id, document_type, version_no),
    CONSTRAINT fk_clinical_document_encounter FOREIGN KEY (encounter_id) REFERENCES clinical_encounter(id),
    CONSTRAINT fk_clinical_document_staff FOREIGN KEY (created_by_staff_id) REFERENCES staff_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 跨业务复用的有限期幂等记录
CREATE TABLE idempotency_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operation_scope VARCHAR(64) NOT NULL,
    actor_id VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    resource_id VARCHAR(128) NOT NULL,
    response_json TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotency_scope_actor_key (operation_scope, actor_id, idempotency_key),
    KEY idx_idempotency_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
