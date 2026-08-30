-- V3：记录首次登录改密要求及密码修改时间，支撑演示账号与正式账号的生命周期差异。
ALTER TABLE app_user
    ADD COLUMN must_change_password BIT NOT NULL DEFAULT 0 AFTER patient_id,
    ADD COLUMN password_changed_at DATETIME(6) NULL AFTER must_change_password;

UPDATE app_user
SET password_changed_at = CURRENT_TIMESTAMP(6)
WHERE password_changed_at IS NULL;
