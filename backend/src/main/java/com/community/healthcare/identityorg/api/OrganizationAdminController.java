package com.community.healthcare.identityorg.api;

import com.community.healthcare.identityorg.application.OrganizationAdminService;
import com.community.healthcare.identityorg.application.OrganizationViews.DepartmentView;
import com.community.healthcare.identityorg.application.OrganizationViews.OrganizationView;
import com.community.healthcare.identityorg.application.OrganizationViews.SiteView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端组织、服务站和科室维护接口。
 *
 * <p>URL 位于管理员安全域；Controller 只完成输入校验和身份传递，层级一致性与审计由应用服务处理。</p>
 */
@RestController
@RequestMapping("/api/v1/admin")
public class OrganizationAdminController {
    private final OrganizationAdminService service;
    public OrganizationAdminController(OrganizationAdminService service) { this.service = service; }

    @PostMapping("/organizations") @ResponseStatus(HttpStatus.CREATED)
    OrganizationView createOrganization(@Valid @RequestBody OrganizationRequest request, Authentication auth) {
        return service.createOrganization(request.code(), request.name(), request.parentOrganizationId(), auth.getName(), role(auth));
    }
    @GetMapping("/organizations") List<OrganizationView> organizations() { return service.organizations(); }

    @PostMapping("/sites") @ResponseStatus(HttpStatus.CREATED)
    SiteView createSite(@Valid @RequestBody SiteRequest request, Authentication auth) {
        return service.createSite(request.organizationId(), request.code(), request.name(), request.siteType(), request.address(), auth.getName(), role(auth));
    }
    @GetMapping("/sites") List<SiteView> sites() { return service.sites(); }

    @PostMapping("/departments") @ResponseStatus(HttpStatus.CREATED)
    DepartmentView createDepartment(@Valid @RequestBody DepartmentRequest request, Authentication auth) {
        return service.createDepartment(request.organizationId(), request.siteId(), request.code(), request.name(), auth.getName(), role(auth));
    }
    @GetMapping("/departments") List<DepartmentView> departments() { return service.departments(); }

    private String role(Authentication auth) { return auth.getAuthorities().stream().findFirst().map(Object::toString).orElse("").replace("ROLE_", ""); }
}

/** 新建机构的输入，其中上级机构为空表示根机构。 */
record OrganizationRequest(@NotBlank @Size(max = 64) String code, @NotBlank @Size(max = 128) String name,
                           Long parentOrganizationId) {}
/** 新建服务站的输入，站点编码在所属机构内保持唯一。 */
record SiteRequest(@NotNull Long organizationId, @NotBlank @Size(max = 64) String code,
                   @NotBlank @Size(max = 128) String name, @NotBlank @Size(max = 32) String siteType,
                   @Size(max = 255) String address) {}
/** 新建科室的输入，机构与站点必须处于同一组织层级。 */
record DepartmentRequest(@NotNull Long organizationId, @NotNull Long siteId,
                         @NotBlank @Size(max = 64) String code, @NotBlank @Size(max = 128) String name) {}
