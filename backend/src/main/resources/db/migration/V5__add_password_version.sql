-- V5：增加密码版本号；改密后可使此前签发的 JWT 立即失效。
ALTER TABLE app_user
    ADD COLUMN password_version BIGINT NOT NULL DEFAULT 0 AFTER password_changed_at;
