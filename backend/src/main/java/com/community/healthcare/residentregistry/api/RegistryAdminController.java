package com.community.healthcare.residentregistry.api;

import com.community.healthcare.residentregistry.application.RegistryApplicationService;
import com.community.healthcare.residentregistry.application.RegistryViews.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * 管理端居民标识、监护关系核验和显式访问授权接口。
 *
 * <p>Controller 传递真实操作者用于审计；证件原值在进入应用层后立即转换为受保护标识。</p>
 */
@RestController
@RequestMapping("/api/v1/admin")
public class RegistryAdminController {
    private final RegistryApplicationService service;
    public RegistryAdminController(RegistryApplicationService service) { this.service = service; }

    @PostMapping("/patients/{patientId}/identifiers") @ResponseStatus(HttpStatus.CREATED)
    IdentifierView addIdentifier(@PathVariable long patientId, @Valid @RequestBody IdentifierRequest request,
                                 Authentication auth) {
        return service.addIdentifier(patientId, request.type(), request.value(), auth.getName(), role(auth));
    }

    @PatchMapping("/guardian-relationships/{id}/verify")
    GuardianView verify(@PathVariable long id, @Valid @RequestBody GuardianVerificationRequest request,
                        Authentication auth) {
        return service.verifyGuardian(id, request.evidenceReference(), auth.getName(), role(auth));
    }

    @PatchMapping("/guardian-relationships/{id}/revoke")
    GuardianView revoke(@PathVariable long id, Authentication auth) {
        return service.revokeGuardian(id, auth.getName(), role(auth));
    }

    @PostMapping("/patient-access-grants") @ResponseStatus(HttpStatus.CREATED)
    GrantView grant(@Valid @RequestBody GrantRequest request, Authentication auth) {
        return service.grant(request.granteeUserId(), request.patientId(), request.purpose(),
                request.scopeCode(), request.validFrom(), request.validTo(), auth.getName(), role(auth));
    }

    @PatchMapping("/patient-access-grants/{id}/revoke")
    GrantView revokeGrant(@PathVariable long id, Authentication auth) {
        return service.revokeGrant(id, auth.getName(), role(auth));
    }

    private String role(Authentication auth) { return auth.getAuthorities().stream().findFirst().map(Object::toString).orElse("").replace("ROLE_", ""); }
}

/** 新增居民身份标识的输入，原始值不会写入数据库。 */
record IdentifierRequest(@NotBlank @Size(max = 32) String type, @NotBlank @Size(max = 256) String value) {}
/** 核验监护关系时可补充的证据引用，不用于存放证件正文。 */
record GuardianVerificationRequest(@Size(max = 255) String evidenceReference) {}
/** 限定用途、范围和有效期的居民数据访问授权。 */
record GrantRequest(@NotNull Long granteeUserId, @NotNull Long patientId,
                    @NotBlank @Size(max = 128) String purpose,
                    @NotBlank @Size(max = 64) String scopeCode,
                    @NotNull Instant validFrom, Instant validTo) {}
