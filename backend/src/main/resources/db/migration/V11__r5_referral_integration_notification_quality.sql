-- V11 / R5：建立双向转诊、可靠外部交换、居民互动和质量管理闭环。
-- 转诊主记录、状态历史、资料和居民反馈
CREATE TABLE referral_case (
    id BIGINT NOT NULL AUTO_INCREMENT, patient_id BIGINT NOT NULL, created_by_staff_id BIGINT NOT NULL,
    encounter_id BIGINT NULL, target_organization VARCHAR(255) NOT NULL, target_department VARCHAR(128) NOT NULL,
    reason VARCHAR(1000) NOT NULL, status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    idempotency_key VARCHAR(128) NOT NULL, consented_at DATETIME(6) NULL, submitted_at DATETIME(6) NULL,
    closed_at DATETIME(6) NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (id), UNIQUE KEY uk_referral_idempotency (idempotency_key),
    KEY idx_referral_patient (patient_id, created_at), KEY idx_referral_staff (created_by_staff_id, status),
    CONSTRAINT fk_referral_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_referral_staff FOREIGN KEY (created_by_staff_id) REFERENCES staff_profile(id),
    CONSTRAINT fk_referral_encounter FOREIGN KEY (encounter_id) REFERENCES clinical_encounter(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE referral_history (
    id BIGINT NOT NULL AUTO_INCREMENT, referral_id BIGINT NOT NULL, from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL, actor_type VARCHAR(32) NOT NULL, actor_id BIGINT NOT NULL,
    note VARCHAR(1000) NULL, occurred_at DATETIME(6) NOT NULL, PRIMARY KEY (id),
    KEY idx_referral_history (referral_id, occurred_at),
    CONSTRAINT fk_referral_history_case FOREIGN KEY (referral_id) REFERENCES referral_case(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE referral_document (
    id BIGINT NOT NULL AUTO_INCREMENT, referral_id BIGINT NOT NULL, document_type VARCHAR(64) NOT NULL,
    storage_reference VARCHAR(500) NOT NULL, sha256 CHAR(64) NOT NULL, uploaded_by_staff_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL, PRIMARY KEY (id), KEY idx_referral_document (referral_id),
    CONSTRAINT fk_referral_document_case FOREIGN KEY (referral_id) REFERENCES referral_case(id),
    CONSTRAINT fk_referral_document_staff FOREIGN KEY (uploaded_by_staff_id) REFERENCES staff_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE referral_feedback (
    id BIGINT NOT NULL AUTO_INCREMENT, referral_id BIGINT NOT NULL, patient_id BIGINT NOT NULL,
    rating INT NOT NULL, comments VARCHAR(1000) NULL, created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_referral_feedback (referral_id, patient_id),
    CONSTRAINT fk_referral_feedback_case FOREIGN KEY (referral_id) REFERENCES referral_case(id),
    CONSTRAINT fk_referral_feedback_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT chk_referral_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 本地事务出站表、交换日志、死信和转诊交换关联，用于幂等重试与对账
CREATE TABLE outbox_event (
    id BIGINT NOT NULL AUTO_INCREMENT, event_key VARCHAR(128) NOT NULL, aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(128) NOT NULL, event_type VARCHAR(64) NOT NULL, payload_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING', attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NULL, last_error VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL, processed_at DATETIME(6) NULL, PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_event_key (event_key), KEY idx_outbox_pending (status, next_attempt_at, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE integration_exchange (
    id BIGINT NOT NULL AUTO_INCREMENT, outbox_event_id BIGINT NOT NULL, adapter_code VARCHAR(64) NOT NULL,
    request_json TEXT NOT NULL, response_json TEXT NULL, status VARCHAR(32) NOT NULL,
    external_reference VARCHAR(128) NULL, attempted_at DATETIME(6) NOT NULL, PRIMARY KEY (id),
    KEY idx_exchange_outbox (outbox_event_id, attempted_at),
    CONSTRAINT fk_exchange_outbox FOREIGN KEY (outbox_event_id) REFERENCES outbox_event(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE integration_dead_letter (
    id BIGINT NOT NULL AUTO_INCREMENT, outbox_event_id BIGINT NOT NULL, payload_json TEXT NOT NULL,
    failure_reason VARCHAR(1000) NOT NULL, attempts INT NOT NULL, failed_at DATETIME(6) NOT NULL,
    resolved_at DATETIME(6) NULL, resolved_by VARCHAR(128) NULL, PRIMARY KEY (id),
    UNIQUE KEY uk_dead_letter_outbox (outbox_event_id),
    CONSTRAINT fk_dead_letter_outbox FOREIGN KEY (outbox_event_id) REFERENCES outbox_event(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE referral_exchange_link (
    id BIGINT NOT NULL AUTO_INCREMENT, referral_id BIGINT NOT NULL, integration_exchange_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL, PRIMARY KEY (id), UNIQUE KEY uk_referral_exchange (referral_id, integration_exchange_id),
    CONSTRAINT fk_referral_exchange_case FOREIGN KEY (referral_id) REFERENCES referral_case(id),
    CONSTRAINT fk_referral_exchange_exchange FOREIGN KEY (integration_exchange_id) REFERENCES integration_exchange(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 居民资料授权和非诊断性健康咨询消息
CREATE TABLE record_release (
    id BIGINT NOT NULL AUTO_INCREMENT, patient_id BIGINT NOT NULL, referral_id BIGINT NULL,
    scope_code VARCHAR(64) NOT NULL, purpose VARCHAR(255) NOT NULL, status VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    requested_at DATETIME(6) NOT NULL, expires_at DATETIME(6) NULL, released_at DATETIME(6) NULL,
    revoked_at DATETIME(6) NULL, PRIMARY KEY (id), KEY idx_record_release_patient (patient_id, status),
    CONSTRAINT fk_record_release_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_record_release_referral FOREIGN KEY (referral_id) REFERENCES referral_case(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE notification_message (
    id BIGINT NOT NULL AUTO_INCREMENT, patient_id BIGINT NOT NULL, direction VARCHAR(16) NOT NULL,
    category VARCHAR(64) NOT NULL, subject VARCHAR(255) NOT NULL, body VARCHAR(2000) NOT NULL,
    diagnostic BIT NOT NULL DEFAULT 0, status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    created_at DATETIME(6) NOT NULL, PRIMARY KEY (id), KEY idx_message_patient (patient_id, created_at),
    CONSTRAINT fk_message_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT chk_message_nondiagnostic CHECK (diagnostic = 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE notification_delivery (
    id BIGINT NOT NULL AUTO_INCREMENT, message_id BIGINT NOT NULL, channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL, attempts INT NOT NULL DEFAULT 0, delivered_at DATETIME(6) NULL,
    last_error VARCHAR(1000) NULL, PRIMARY KEY (id), KEY idx_delivery_status (status, attempts),
    CONSTRAINT fk_delivery_message FOREIGN KEY (message_id) REFERENCES notification_message(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 通用服务评价及按期间生成的质量快照
CREATE TABLE service_feedback (
    id BIGINT NOT NULL AUTO_INCREMENT, patient_id BIGINT NOT NULL, business_type VARCHAR(64) NOT NULL,
    business_id VARCHAR(128) NOT NULL, rating INT NOT NULL, comments VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL, PRIMARY KEY (id),
    UNIQUE KEY uk_service_feedback_once (patient_id, business_type, business_id),
    CONSTRAINT fk_service_feedback_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT chk_service_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE quality_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT, period_key VARCHAR(32) NOT NULL, metric_code VARCHAR(64) NOT NULL,
    metric_value DECIMAL(18,4) NOT NULL, numerator BIGINT NULL, denominator BIGINT NULL,
    generated_at DATETIME(6) NOT NULL, PRIMARY KEY (id),
    UNIQUE KEY uk_quality_snapshot_metric (period_key, metric_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
