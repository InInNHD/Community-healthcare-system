-- V10 / R4：建立家庭医生签约履约与成人慢病、老年人公卫随访闭环。
-- 家庭医生团队及其成员
CREATE TABLE care_team (
    id BIGINT NOT NULL AUTO_INCREMENT, organization_id BIGINT NOT NULL, site_id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL, name VARCHAR(128) NOT NULL, active BIT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_care_team_site_code (site_id, code),
    KEY idx_care_team_org_active (organization_id, active),
    CONSTRAINT fk_care_team_org FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT fk_care_team_site FOREIGN KEY (site_id) REFERENCES site(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE care_team_member (
    id BIGINT NOT NULL AUTO_INCREMENT, team_id BIGINT NOT NULL, staff_profile_id BIGINT NOT NULL,
    member_role VARCHAR(32) NOT NULL, active BIT NOT NULL DEFAULT 1,
    joined_at DATETIME(6) NOT NULL, left_at DATETIME(6) NULL, version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_care_team_member (team_id, staff_profile_id),
    KEY idx_care_team_member_staff (staff_profile_id, active),
    CONSTRAINT fk_care_team_member_team FOREIGN KEY (team_id) REFERENCES care_team(id),
    CONSTRAINT fk_care_team_member_staff FOREIGN KEY (staff_profile_id) REFERENCES staff_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 带版本的服务包和项目快照
CREATE TABLE service_package (
    id BIGINT NOT NULL AUTO_INCREMENT, organization_id BIGINT NOT NULL, code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL, version_no INT NOT NULL, active BIT NOT NULL DEFAULT 1,
    effective_from DATE NOT NULL, effective_to DATE NULL,
    created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_service_package_version (organization_id, code, version_no),
    KEY idx_service_package_active (organization_id, active, effective_from),
    CONSTRAINT fk_service_package_org FOREIGN KEY (organization_id) REFERENCES organization(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE service_package_item (
    id BIGINT NOT NULL AUTO_INCREMENT, package_id BIGINT NOT NULL, item_code VARCHAR(64) NOT NULL,
    item_name VARCHAR(128) NOT NULL, target_population VARCHAR(32) NULL,
    frequency_rule VARCHAR(255) NOT NULL, created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_service_package_item (package_id, item_code),
    CONSTRAINT fk_service_package_item_package FOREIGN KEY (package_id) REFERENCES service_package(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 居民确认的签约合同、状态历史、服务任务与履约结果
CREATE TABLE fd_contract (
    id BIGINT NOT NULL AUTO_INCREMENT, patient_id BIGINT NOT NULL, team_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL, status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    starts_on DATE NOT NULL, ends_on DATE NOT NULL, resident_confirmed_at DATETIME(6) NULL,
    created_by_staff_id BIGINT NOT NULL, idempotency_key VARCHAR(128) NOT NULL,
    created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_fd_contract_idempotency (idempotency_key),
    KEY idx_fd_contract_patient_status (patient_id, status), KEY idx_fd_contract_team_status (team_id, status),
    CONSTRAINT fk_fd_contract_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_fd_contract_team FOREIGN KEY (team_id) REFERENCES care_team(id),
    CONSTRAINT fk_fd_contract_package FOREIGN KEY (package_id) REFERENCES service_package(id),
    CONSTRAINT fk_fd_contract_creator FOREIGN KEY (created_by_staff_id) REFERENCES staff_profile(id),
    CONSTRAINT chk_fd_contract_period CHECK (ends_on >= starts_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE fd_contract_history (
    id BIGINT NOT NULL AUTO_INCREMENT, contract_id BIGINT NOT NULL, from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL, actor_type VARCHAR(32) NOT NULL, actor_id BIGINT NOT NULL,
    reason VARCHAR(500) NULL, occurred_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id), KEY idx_fd_contract_history (contract_id, occurred_at),
    CONSTRAINT fk_fd_contract_history_contract FOREIGN KEY (contract_id) REFERENCES fd_contract(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE fd_service_task (
    id BIGINT NOT NULL AUTO_INCREMENT, contract_id BIGINT NOT NULL, patient_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL, assigned_staff_id BIGINT NULL, task_type VARCHAR(64) NOT NULL,
    source_type VARCHAR(32) NOT NULL, source_id BIGINT NULL, due_at DATETIME(6) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_ASSIGNMENT', idempotency_key VARCHAR(128) NOT NULL,
    created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_fd_task_idempotency (idempotency_key),
    KEY idx_fd_task_team_queue (team_id, status, due_at), KEY idx_fd_task_patient (patient_id, created_at),
    CONSTRAINT fk_fd_task_contract FOREIGN KEY (contract_id) REFERENCES fd_contract(id),
    CONSTRAINT fk_fd_task_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_fd_task_team FOREIGN KEY (team_id) REFERENCES care_team(id),
    CONSTRAINT fk_fd_task_assignee FOREIGN KEY (assigned_staff_id) REFERENCES staff_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE fd_service_task_history (
    id BIGINT NOT NULL AUTO_INCREMENT, task_id BIGINT NOT NULL, from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL, actor_staff_id BIGINT NULL, reason VARCHAR(500) NULL,
    occurred_at DATETIME(6) NOT NULL, PRIMARY KEY (id), KEY idx_fd_task_history (task_id, occurred_at),
    CONSTRAINT fk_fd_task_history_task FOREIGN KEY (task_id) REFERENCES fd_service_task(id),
    CONSTRAINT fk_fd_task_history_staff FOREIGN KEY (actor_staff_id) REFERENCES staff_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE fd_service_fulfillment (
    id BIGINT NOT NULL AUTO_INCREMENT, task_id BIGINT NOT NULL, fulfilled_by_staff_id BIGINT NOT NULL,
    summary VARCHAR(1000) NOT NULL, fulfilled_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_fd_fulfillment_task (task_id),
    CONSTRAINT fk_fd_fulfillment_task FOREIGN KEY (task_id) REFERENCES fd_service_task(id),
    CONSTRAINT fk_fd_fulfillment_staff FOREIGN KEY (fulfilled_by_staff_id) REFERENCES staff_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 明确非诊断性的公卫规则版本
CREATE TABLE ph_rule_version (
    id BIGINT NOT NULL AUTO_INCREMENT, rule_code VARCHAR(64) NOT NULL, version_no INT NOT NULL,
    population_type VARCHAR(32) NOT NULL, expression_json TEXT NOT NULL, action_json TEXT NOT NULL,
    diagnostic BIT NOT NULL DEFAULT 0, active BIT NOT NULL DEFAULT 1,
    effective_from DATE NOT NULL, effective_to DATE NULL, created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_ph_rule_version (rule_code, version_no),
    KEY idx_ph_rule_active (population_type, active, effective_from),
    CONSTRAINT chk_ph_rule_nondiagnostic CHECK (diagnostic = 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 重点人群登记、周期随访计划和随访审核
CREATE TABLE ph_registry (
    id BIGINT NOT NULL AUTO_INCREMENT, patient_id BIGINT NOT NULL, population_type VARCHAR(32) NOT NULL,
    managing_team_id BIGINT NOT NULL, status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    enrolled_on DATE NOT NULL, exited_on DATE NULL, idempotency_key VARCHAR(128) NOT NULL,
    created_by_staff_id BIGINT NOT NULL, created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_ph_registry_patient_type (patient_id, population_type),
    UNIQUE KEY uk_ph_registry_idempotency (idempotency_key), KEY idx_ph_registry_team (managing_team_id, status, population_type),
    CONSTRAINT fk_ph_registry_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_ph_registry_team FOREIGN KEY (managing_team_id) REFERENCES care_team(id),
    CONSTRAINT fk_ph_registry_creator FOREIGN KEY (created_by_staff_id) REFERENCES staff_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ph_follow_up_plan (
    id BIGINT NOT NULL AUTO_INCREMENT, registry_id BIGINT NOT NULL, plan_code VARCHAR(64) NOT NULL,
    cadence_days INT NOT NULL, next_due_on DATE NOT NULL, status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_ph_plan_registry_code (registry_id, plan_code),
    KEY idx_ph_plan_due (status, next_due_on),
    CONSTRAINT fk_ph_plan_registry FOREIGN KEY (registry_id) REFERENCES ph_registry(id),
    CONSTRAINT chk_ph_plan_cadence CHECK (cadence_days > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ph_follow_up_visit (
    id BIGINT NOT NULL AUTO_INCREMENT, plan_id BIGINT NOT NULL, registry_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL, performed_by_staff_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT', findings_json TEXT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL, submitted_at DATETIME(6) NULL,
    verified_by_staff_id BIGINT NULL, verified_at DATETIME(6) NULL, return_reason VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_ph_visit_idempotency (idempotency_key),
    KEY idx_ph_visit_registry (registry_id, status, created_at), KEY idx_ph_visit_patient (patient_id, created_at),
    CONSTRAINT fk_ph_visit_plan FOREIGN KEY (plan_id) REFERENCES ph_follow_up_plan(id),
    CONSTRAINT fk_ph_visit_registry FOREIGN KEY (registry_id) REFERENCES ph_registry(id),
    CONSTRAINT fk_ph_visit_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_ph_visit_performer FOREIGN KEY (performed_by_staff_id) REFERENCES staff_profile(id),
    CONSTRAINT fk_ph_visit_verifier FOREIGN KEY (verified_by_staff_id) REFERENCES staff_profile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 风险评估与可追踪处置的健康告警
CREATE TABLE ph_risk_assessment (
    id BIGINT NOT NULL AUTO_INCREMENT, registry_id BIGINT NOT NULL, rule_version_id BIGINT NOT NULL,
    risk_level VARCHAR(32) NOT NULL, evidence_json TEXT NOT NULL, diagnostic BIT NOT NULL DEFAULT 0,
    evaluated_by_staff_id BIGINT NULL, evaluated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id), KEY idx_ph_risk_registry (registry_id, evaluated_at),
    CONSTRAINT fk_ph_risk_registry FOREIGN KEY (registry_id) REFERENCES ph_registry(id),
    CONSTRAINT fk_ph_risk_rule FOREIGN KEY (rule_version_id) REFERENCES ph_rule_version(id),
    CONSTRAINT fk_ph_risk_staff FOREIGN KEY (evaluated_by_staff_id) REFERENCES staff_profile(id),
    CONSTRAINT chk_ph_risk_nondiagnostic CHECK (diagnostic = 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ph_health_alert (
    id BIGINT NOT NULL AUTO_INCREMENT, registry_id BIGINT NOT NULL, patient_id BIGINT NOT NULL,
    rule_version_id BIGINT NULL, severity VARCHAR(32) NOT NULL, status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    message VARCHAR(1000) NOT NULL, non_diagnostic BIT NOT NULL DEFAULT 1, idempotency_key VARCHAR(128) NOT NULL,
    acknowledged_by_staff_id BIGINT NULL, acknowledged_at DATETIME(6) NULL,
    closed_by_staff_id BIGINT NULL, closed_at DATETIME(6) NULL, closure_note VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id), UNIQUE KEY uk_ph_alert_idempotency (idempotency_key),
    KEY idx_ph_alert_queue (status, severity, created_at), KEY idx_ph_alert_patient (patient_id, created_at),
    CONSTRAINT fk_ph_alert_registry FOREIGN KEY (registry_id) REFERENCES ph_registry(id),
    CONSTRAINT fk_ph_alert_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_ph_alert_rule FOREIGN KEY (rule_version_id) REFERENCES ph_rule_version(id),
    CONSTRAINT fk_ph_alert_ack_staff FOREIGN KEY (acknowledged_by_staff_id) REFERENCES staff_profile(id),
    CONSTRAINT fk_ph_alert_close_staff FOREIGN KEY (closed_by_staff_id) REFERENCES staff_profile(id),
    CONSTRAINT chk_ph_alert_nondiagnostic CHECK (non_diagnostic = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
