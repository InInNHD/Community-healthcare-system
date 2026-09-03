-- V6 / R1：建立机构、站点、科室、医护任职、居民标识保护、访问授权和统一审计基础。
-- 机构层级：当前支持单中心多站点，organization_id 为未来多机构隔离预留。
CREATE TABLE organization (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    parent_organization_id BIGINT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_organization_code (code),
    -- MySQL 不允许 CHECK 表达式引用 AUTO_INCREMENT 列；自引用父子校验由 OrganizationHierarchyPolicy 执行。
    CONSTRAINT fk_organization_parent FOREIGN KEY (parent_organization_id) REFERENCES organization(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE site (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    site_type VARCHAR(32) NOT NULL,
    address VARCHAR(255) NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_site_org_code (organization_id, code),
    KEY idx_site_organization (organization_id),
    CONSTRAINT fk_site_organization FOREIGN KEY (organization_id) REFERENCES organization(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE department (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_department_site_code (site_id, code),
    KEY idx_department_organization (organization_id),
    CONSTRAINT fk_department_organization FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT fk_department_site FOREIGN KEY (site_id) REFERENCES site(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 医护档案与带有效期的站点/科室任职
CREATE TABLE staff_profile (
    id BIGINT NOT NULL AUTO_INCREMENT,
    organization_id BIGINT NOT NULL,
    staff_no VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    staff_type VARCHAR(32) NOT NULL,
    account_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_staff_profile_org_no (organization_id, staff_no),
    CONSTRAINT fk_staff_profile_organization FOREIGN KEY (organization_id) REFERENCES organization(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE staff_site_assignment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    staff_profile_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    department_id BIGINT NULL,
    role_code VARCHAR(32) NOT NULL,
    valid_from DATETIME(6) NOT NULL,
    valid_to DATETIME(6) NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_staff_site_assignment_staff (staff_profile_id, active),
    KEY idx_staff_site_assignment_site (site_id, active),
    CONSTRAINT fk_staff_site_assignment_staff FOREIGN KEY (staff_profile_id) REFERENCES staff_profile(id),
    CONSTRAINT fk_staff_site_assignment_site FOREIGN KEY (site_id) REFERENCES site(id),
    CONSTRAINT fk_staff_site_assignment_department FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 居民敏感标识仅保存密文和检索哈希，不保存可直接检索的证件明文
CREATE TABLE patient_identifier (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patient_id BIGINT NOT NULL,
    identifier_type VARCHAR(32) NOT NULL,
    identifier_hash CHAR(64) NOT NULL,
    masked_value VARCHAR(128) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_patient_identifier_type_hash (identifier_type, identifier_hash),
    KEY idx_patient_identifier_patient (patient_id, active),
    CONSTRAINT fk_patient_identifier_patient FOREIGN KEY (patient_id) REFERENCES patient(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE patient_site_enrollment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patient_id BIGINT NOT NULL,
    site_id BIGINT NOT NULL,
    enrolled_at DATETIME(6) NOT NULL,
    ended_at DATETIME(6) NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_patient_site_enrollment_patient (patient_id, active),
    KEY idx_patient_site_enrollment_site (site_id, active),
    CONSTRAINT fk_patient_site_enrollment_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_patient_site_enrollment_site FOREIGN KEY (site_id) REFERENCES site(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 监护关系和临时访问授权；所有查询仍需在应用层执行默认拒绝的数据范围校验
CREATE TABLE guardian_relationship (
    id BIGINT NOT NULL AUTO_INCREMENT,
    guardian_patient_id BIGINT NOT NULL,
    dependent_patient_id BIGINT NOT NULL,
    relationship_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    evidence_reference VARCHAR(255) NULL,
    requested_at DATETIME(6) NOT NULL,
    verified_at DATETIME(6) NULL,
    verified_by VARCHAR(128) NULL,
    revoked_at DATETIME(6) NULL,
    revoked_by VARCHAR(128) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_guardian_relationship_guardian (guardian_patient_id, status),
    KEY idx_guardian_relationship_dependent (dependent_patient_id, status),
    CONSTRAINT fk_guardian_relationship_guardian FOREIGN KEY (guardian_patient_id) REFERENCES patient(id),
    CONSTRAINT fk_guardian_relationship_dependent FOREIGN KEY (dependent_patient_id) REFERENCES patient(id),
    CONSTRAINT chk_guardian_relationship_distinct CHECK (guardian_patient_id <> dependent_patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE patient_access_grant (
    id BIGINT NOT NULL AUTO_INCREMENT,
    grantee_user_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    purpose VARCHAR(128) NOT NULL,
    scope_code VARCHAR(64) NOT NULL,
    valid_from DATETIME(6) NOT NULL,
    valid_to DATETIME(6) NULL,
    revoked_at DATETIME(6) NULL,
    granted_by VARCHAR(128) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_patient_access_grant_lookup (grantee_user_id, patient_id, revoked_at),
    CONSTRAINT fk_patient_access_grant_user FOREIGN KEY (grantee_user_id) REFERENCES app_user(id),
    CONSTRAINT fk_patient_access_grant_patient FOREIGN KEY (patient_id) REFERENCES patient(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 追加式安全与业务审计事件
CREATE TABLE audit_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    occurred_at DATETIME(6) NOT NULL,
    actor VARCHAR(128) NOT NULL,
    actor_role VARCHAR(32) NULL,
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(128) NULL,
    outcome VARCHAR(32) NOT NULL,
    purpose VARCHAR(255) NULL,
    details_json TEXT NULL,
    correlation_id VARCHAR(64) NULL,
    PRIMARY KEY (id),
    KEY idx_audit_event_time (occurred_at),
    KEY idx_audit_event_resource (resource_type, resource_id),
    KEY idx_audit_event_actor (actor, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 将统一账号关联到新医护档案，并为授权版本、MFA 和账号状态预留字段
ALTER TABLE app_user ADD COLUMN account_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE app_user ADD COLUMN authz_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE app_user ADD COLUMN mfa_required BIT NOT NULL DEFAULT 0;
ALTER TABLE app_user ADD COLUMN mfa_enrolled_at DATETIME(6) NULL;
ALTER TABLE app_user ADD COLUMN mfa_secret_ciphertext VARCHAR(1024) NULL;
ALTER TABLE app_user ADD COLUMN staff_profile_id BIGINT NULL;
CREATE INDEX idx_app_user_staff_profile ON app_user (staff_profile_id);
ALTER TABLE app_user ADD CONSTRAINT fk_app_user_staff_profile FOREIGN KEY (staff_profile_id) REFERENCES staff_profile(id);
