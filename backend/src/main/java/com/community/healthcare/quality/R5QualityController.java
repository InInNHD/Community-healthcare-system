package com.community.healthcare.quality;

import com.community.healthcare.referral.application.R5PlatformService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 管理端服务质量指标刷新与历史快照查询入口。 */
@RestController
@RequestMapping("/api/v1/admin/quality")
class R5QualityController {
    private final R5PlatformService service;
    R5QualityController(R5PlatformService service){this.service=service;}
    @PostMapping("/snapshots/refresh") R5PlatformService.QualityView refresh(){return service.refreshQuality();}
    @GetMapping("/snapshots") List<Map<String,Object>> snapshots(){return service.qualitySnapshots();}
}
