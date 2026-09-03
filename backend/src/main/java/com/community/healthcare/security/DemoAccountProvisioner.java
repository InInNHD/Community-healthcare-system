package com.community.healthcare.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 在对应医护和居民档案已创建后，幂等补齐演示门户账号。
 *
 * <p>仅在显式启用 Bootstrap 的非生产 Profile 中生效；已存在账号不会被覆盖，
 * 因而不会在应用重启时重置用户修改后的密码。</p>
 */
@Service
public class DemoAccountProvisioner {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final SecurityProperties properties;

    DemoAccountProvisioner(AppUserRepository users, PasswordEncoder encoder, SecurityProperties properties) {
        this.users = users;
        this.encoder = encoder;
        this.properties = properties;
    }

    /**
     * 为默认演示人员建立账号与业务主体关联。
     *
     * @param doctorId 演示医生标识
     * @param nurseId 演示护士标识
     * @param pharmacistId 演示药师标识
     * @param registrarId 演示挂号收费人员标识
     * @param residentId 演示居民档案标识
     */
    public void ensureDemoAccounts(Long residentId, Long doctorId, Long nurseId,
                                   Long pharmacistId, Long registrarId) {
        SecurityProperties.Bootstrap bootstrap = properties.bootstrap();
        if (!bootstrap.enabled()) return;
        ensure("doctor", bootstrap.doctorPassword(), "王医生", AppRole.DOCTOR, doctorId, null);
        ensure("nurse", bootstrap.nursePassword(), "刘护士", AppRole.NURSE, nurseId, null);
        ensure("pharmacist", bootstrap.pharmacistPassword(), "赵药师", AppRole.PHARMACIST, pharmacistId, null);
        ensure("registrar", bootstrap.registrarPassword(), "陈收费员", AppRole.REGISTRAR, registrarId, null);
        ensure("resident", bootstrap.residentPassword(), "张明", AppRole.RESIDENT, null, residentId);
    }

    private void ensure(String username, String password, String displayName, AppRole role,
                        Long staffId, Long patientId) {
        if (users.findByUsername(username).isEmpty()) {
            users.save(new AppUser(username, encoder.encode(password), displayName, role, staffId, patientId,
                    properties.bootstrap().mustChangePassword()));
        }
    }
}
