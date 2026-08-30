package com.community.healthcare.residentregistry.api;

import com.community.healthcare.residentregistry.application.PatientAccessSubject;
import com.community.healthcare.residentregistry.application.RegistryApplicationService;
import com.community.healthcare.residentregistry.application.RegistryViews.GuardianView;
import com.community.healthcare.residentregistry.application.RegistryViews.PatientBasicProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

/**
 * 居民发起监护关系申请并访问家庭成员基础档案的接口。
 *
 * <p>居民身份从已验证 JWT 的 patientId 获取，不能由请求正文指定；家庭档案查询还需通过
 * 已核验监护关系或其他显式授权策略。</p>
 */
@RestController
@RequestMapping("/api/v1/resident")
public class ResidentRegistryController {
    private final RegistryApplicationService service;
    public ResidentRegistryController(RegistryApplicationService service) { this.service = service; }

    @PostMapping("/guardian-relationships") @ResponseStatus(HttpStatus.CREATED)
    GuardianView requestGuardian(@AuthenticationPrincipal Jwt jwt,
                                 @Valid @RequestBody GuardianRequest request, Authentication auth) {
        return service.requestGuardian(requiredLong(jwt, "patientId"), request.dependentPatientId(),
                request.relationshipType(), request.evidenceReference(), auth.getName(), "RESIDENT");
    }

    @GetMapping("/family/{patientId}")
    PatientBasicProfile familyProfile(@AuthenticationPrincipal Jwt jwt, @PathVariable long patientId) {
        return service.familyProfile(subject(jwt), patientId);
    }

    private PatientAccessSubject subject(Jwt jwt) {
        Set<String> roles = new HashSet<>();
        if (jwt.getClaimAsStringList("roles") != null) roles.addAll(jwt.getClaimAsStringList("roles"));
        return new PatientAccessSubject(optionalLong(jwt, "userId"), optionalLong(jwt, "patientId"),
                optionalLong(jwt, "staffProfileId"), roles);
    }
    private Long requiredLong(Jwt jwt, String claim) {
        Long value = optionalLong(jwt, claim);
        if (value == null) throw new org.springframework.security.access.AccessDeniedException("账号未关联居民档案");
        return value;
    }
    private Long optionalLong(Jwt jwt, String claim) {
        Object value = jwt.getClaim(claim);
        return value instanceof Number number ? number.longValue() : null;
    }
}

/** 居民提交的监护关系申请，后续必须由管理员核验后才授予访问能力。 */
record GuardianRequest(@NotNull Long dependentPatientId,
                       @NotBlank @Size(max = 32) String relationshipType,
                       @Size(max = 255) String evidenceReference) {}
