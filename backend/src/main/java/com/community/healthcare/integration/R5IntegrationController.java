package com.community.healthcare.integration;

import com.community.healthcare.referral.application.R5PlatformService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 管理端外部平台交换工作台，用于查看和人工重试出站事件。 */
@RestController
@RequestMapping("/api/v1/admin/integrations")
class R5IntegrationController {
    private final R5PlatformService service;
    R5IntegrationController(R5PlatformService service){this.service=service;}
    /** 查询待发送、已发送及失败的出站事件。 */
    @GetMapping("/outbox") List<Map<String,Object>> outbox(){return service.outbox();}
    /** 人工重试指定事件，适配器仍按事件键保持幂等。 */
    @PostMapping("/outbox/{id}/retry") R5PlatformService.WorkbenchView retry(@PathVariable long id, Authentication auth){return service.retryOutbox(id,auth.getName());}
}
