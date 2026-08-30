package com.community.healthcare.billing.api;

import com.community.healthcare.pharmacy.infrastructure.R3PharmacyBillingService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 居民端本人账单查询入口。 */
@RestController
@RequestMapping("/api/v1/resident/billing")
public class R3ResidentBillingController {
    private final R3PharmacyBillingService service;
    public R3ResidentBillingController(R3PharmacyBillingService service) { this.service = service; }

    /** 从 JWT 取得居民标识，仅返回当前登录居民的账单。 */
    @GetMapping("/invoices")
    List<R3PharmacyBillingService.InvoiceView> invoices(@AuthenticationPrincipal Jwt jwt) {
        Object value = jwt.getClaim("patientId");
        if (!(value instanceof Number number)) throw new IllegalArgumentException("令牌缺少居民标识");
        return service.residentInvoices(number.longValue());
    }
}
