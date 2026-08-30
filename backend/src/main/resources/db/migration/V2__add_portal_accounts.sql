-- V2：为统一门户账号增加角色与居民/医护业务档案关联。
ALTER TABLE app_user
    ADD COLUMN staff_id BIGINT NULL AFTER role,
    ADD COLUMN patient_id BIGINT NULL AFTER staff_id;

CREATE INDEX idx_app_user_staff ON app_user (staff_id);
CREATE INDEX idx_app_user_patient ON app_user (patient_id);

ALTER TABLE app_user
    ADD CONSTRAINT fk_app_user_staff FOREIGN KEY (staff_id) REFERENCES doctor(id),
    ADD CONSTRAINT fk_app_user_patient FOREIGN KEY (patient_id) REFERENCES patient(id);
