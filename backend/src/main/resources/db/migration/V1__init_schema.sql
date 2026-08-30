-- V1：旧版基础业务表，仅保留历史兼容；新模块通过后续迁移逐步建立标准化模型。
-- 用户与门户身份
CREATE TABLE app_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 居民和医护基础档案（后续迁移会补充统一身份、机构及软删除字段）
CREATE TABLE patient (
    id BIGINT NOT NULL AUTO_INCREMENT,
    id_card VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    gender VARCHAR(16),
    birth_date DATE,
    phone VARCHAR(32),
    address VARCHAR(255),
    balance DECIMAL(12,2) NOT NULL DEFAULT 0,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_patient_id_card (id_card),
    KEY idx_patient_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE doctor (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_no VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    department VARCHAR(64) NOT NULL,
    title VARCHAR(64),
    specialty VARCHAR(500),
    phone VARCHAR(32),
    schedule_summary VARCHAR(255),
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_doctor_employee_no (employee_no),
    KEY idx_doctor_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 旧版预约、健康记录、药品和慢病数据
CREATE TABLE appointment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_no VARCHAR(40) NOT NULL,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    scheduled_at DATETIME(6) NOT NULL,
    status VARCHAR(24) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    remark VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_appointment_no (appointment_no),
    KEY idx_appointment_time (scheduled_at),
    KEY idx_appointment_patient (patient_id),
    CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_appointment_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE health_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patient_id BIGINT NOT NULL,
    recorded_at DATETIME(6) NOT NULL,
    heart_rate INT,
    systolic_pressure INT,
    diastolic_pressure INT,
    blood_oxygen INT,
    weight DECIMAL(5,2),
    note VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_health_patient_time (patient_id, recorded_at),
    CONSTRAINT fk_health_patient FOREIGN KEY (patient_id) REFERENCES patient(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE medicine (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    category VARCHAR(64),
    price DECIMAL(12,2) NOT NULL DEFAULT 0,
    stock INT NOT NULL DEFAULT 0,
    minimum_stock INT NOT NULL DEFAULT 0,
    specification VARCHAR(500),
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_medicine_name (name),
    KEY idx_medicine_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE chronic_case (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patient_id BIGINT NOT NULL,
    disease_type VARCHAR(64) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    diagnosis_date DATE NOT NULL,
    doctor_id BIGINT,
    management_plan VARCHAR(500),
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_chronic_patient (patient_id),
    CONSTRAINT fk_chronic_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_chronic_doctor FOREIGN KEY (doctor_id) REFERENCES doctor(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
