package com.community.healthcare.security;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 处理当前登录账号的密码变更。
 *
 * <p>服务在同一事务内校验旧密码、执行统一强度策略并提升密码版本，
 * 从而使此前签发的 JWT 在后续校验时立即失效。</p>
 */
@Service
class AccountPasswordService {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final PasswordRules rules;

    AccountPasswordService(AppUserRepository users, PasswordEncoder encoder, SecurityProperties properties) {
        this.users = users;
        this.encoder = encoder;
        this.rules = new PasswordRules(properties.password());
    }

    /**
     * 修改账号密码并要求客户端重新认证。
     *
     * @throws jakarta.persistence.EntityNotFoundException 账号不存在或已停用
     * @throws BadCredentialsException 当前密码不匹配
     * @throws IllegalArgumentException 新密码不符合策略或与当前密码相同
     */
    @Transactional
    PasswordChangeResponse change(String username, String currentPassword, String newPassword) {
        AppUser user = users.findByUsername(username).filter(AppUser::isActive)
                .orElseThrow(() -> new EntityNotFoundException("账号不存在"));
        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("当前密码错误");
        }
        rules.requireStrong(newPassword);
        if (encoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("新密码不能与当前密码相同");
        }
        user.changePassword(encoder.encode(newPassword));
        return new PasswordChangeResponse(user.getPasswordChangedAt(), false, true);
    }
}

/** 密码修改结果；成功后客户端必须清除旧会话并重新登录。 */
record PasswordChangeResponse(Instant passwordChangedAt, boolean mustChangePassword,
                              boolean reauthenticationRequired) {}
