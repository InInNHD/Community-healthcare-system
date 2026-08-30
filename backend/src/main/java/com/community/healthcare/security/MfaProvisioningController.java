package com.community.healthcare.security;

import com.community.healthcare.audit.application.AuditEventCommand;
import com.community.healthcare.audit.application.AuditTrail;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;

/** 管理员为非居民账号受控开通 MFA 的接口。 */
@RestController
@RequestMapping("/api/v1/admin/accounts")
class MfaProvisioningController {
    private final MfaProvisioningService provisioning;

    MfaProvisioningController(MfaProvisioningService provisioning) {
        this.provisioning = provisioning;
    }

    @PostMapping("/{username}/mfa/provision")
    @ResponseStatus(HttpStatus.CREATED)
    MfaProvisioningResponse provision(@PathVariable String username, Authentication authentication) {
        return provisioning.provision(username, authentication.getName());
    }
}

/**
 * 生成 TOTP 种子、加密保存并记录开通审计。
 *
 * <p>明文种子只在本次响应中返回，调用端应立即引导用户绑定认证器且不得写入普通日志。</p>
 */
@Service
class MfaProvisioningService {
    private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private final AppUserRepository users;
    private final MfaAuthenticationService mfa;
    private final AuditTrail audit;
    private final SecurityProperties properties;
    private final SecureRandom random = new SecureRandom();

    MfaProvisioningService(AppUserRepository users, MfaAuthenticationService mfa,
                           AuditTrail audit, SecurityProperties properties) {
        this.users = users; this.mfa = mfa; this.audit = audit; this.properties = properties;
    }

    /**
     * 为尚未开通 MFA 的可用工作人员账号生成一次绑定信息。
     *
     * @throws IllegalArgumentException 账号不存在、不可用、属于居民或已经开通 MFA
     */
    @Transactional
    MfaProvisioningResponse provision(String username, String actor) {
        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("账号不存在"));
        if (!user.isAccountUsable()) throw new IllegalArgumentException("账号不可用");
        if (user.getRole() == AppRole.RESIDENT) throw new IllegalArgumentException("居民账号无需工作人员 MFA");
        if (user.getMfaSecretCiphertext() != null) throw new IllegalArgumentException("该账号已完成 MFA 配置");
        byte[] seed = new byte[20];
        random.nextBytes(seed);
        String secret = encodeBase32(seed);
        user.enrollMfa(mfa.encryptForProvisioning(secret));
        users.save(user);
        audit.append(new AuditEventCommand(actor, "ADMIN", "MFA_PROVISION", "APP_USER",
                user.getId().toString(), "SUCCESS", "受控开通工作人员双因素认证", null, null));
        String label = properties.jwt().issuer() + ":" + user.getUsername();
        String uri = "otpauth://totp/" + url(label) + "?secret=" + secret
                + "&issuer=" + url(properties.jwt().issuer()) + "&algorithm=SHA1&digits=6&period=30";
        return new MfaProvisioningResponse(user.getUsername(), secret, uri);
    }

    private String encodeBase32(byte[] input) {
        StringBuilder output = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte value : input) {
            buffer = (buffer << 8) | (value & 0xff);
            bits += 8;
            while (bits >= 5) {
                bits -= 5;
                output.append(BASE32[(buffer >> bits) & 31]);
            }
        }
        if (bits > 0) output.append(BASE32[(buffer << (5 - bits)) & 31]);
        return output.toString().toUpperCase(Locale.ROOT);
    }

    private String url(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}

/**
 * MFA 首次绑定响应。
 *
 * <p>{@code secret} 和 {@code otpauthUri} 均为一次性敏感信息，不应被缓存或记录。</p>
 */
record MfaProvisioningResponse(String username, String secret, String otpauthUri) {}
