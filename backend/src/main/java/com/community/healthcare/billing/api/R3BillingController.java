package com.community.healthcare.billing.api;

import com.community.healthcare.pharmacy.infrastructure.R3PharmacyBillingService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/** 收费员端账单、支付、退款及模拟医保申报入口。 */
@RestController
@RequestMapping("/api/v1/staff/billing")
@PreAuthorize("hasRole('REGISTRAR')")
public class R3BillingController {
    private final R3PharmacyBillingService service;
    public R3BillingController(R3PharmacyBillingService service) { this.service = service; }

    /** 创建收费账单。 */
    @PostMapping("/invoices") @ResponseStatus(HttpStatus.CREATED)
    R3PharmacyBillingService.InvoiceView create(@AuthenticationPrincipal Jwt jwt,
            @RequestBody R3PharmacyBillingService.InvoiceCommand command) {
        return service.createInvoice(staffId(jwt), command);
    }
    @PostMapping("/invoices/{id}/issue")
    R3PharmacyBillingService.InvoiceView issue(@PathVariable long id) { return service.issue(id); }
    /** 以幂等键登记支付，避免支付请求重放造成重复入账。 */
    @PostMapping("/invoices/{id}/payments")
    R3PharmacyBillingService.InvoiceView pay(Authentication auth,@PathVariable long id, @RequestHeader("Idempotency-Key") String key,
            @RequestBody R3PharmacyBillingService.PaymentCommand command) { return service.pay(id, key, command, auth.getName()); }
    /** 以幂等键登记退款。 */
    @PostMapping("/invoices/{id}/refunds")
    R3PharmacyBillingService.InvoiceView refund(@AuthenticationPrincipal Jwt jwt, Authentication auth,
            @PathVariable long id, @RequestHeader("Idempotency-Key") String key,
            @RequestBody R3PharmacyBillingService.RefundCommand command) {
        return service.refund(staffId(jwt), id, key, command, auth.getName());
    }
    @GetMapping("/insurance/eligibility/{patientId}")
    R3PharmacyBillingService.EligibilityView eligibility(@PathVariable long patientId) { return service.eligibility(patientId); }
    @PostMapping("/insurance/claims") @ResponseStatus(HttpStatus.CREATED)
    R3PharmacyBillingService.ClaimView claim(Authentication auth, @RequestHeader("Idempotency-Key") String key,
            @RequestBody R3PharmacyBillingService.ClaimCommand command) { return service.submitClaim(key, command, auth.getName()); }
    @PostMapping("/insurance/claims/{id}/settle")
    R3PharmacyBillingService.ClaimView settle(Authentication auth, @PathVariable long id,
            @RequestBody SettlementCommand command) { return service.settleClaim(id, command.amount(), auth.getName()); }
    @PostMapping("/insurance/claims/{id}/cancel")
    R3PharmacyBillingService.ClaimView cancel(Authentication auth, @PathVariable long id) { return service.cancelClaim(id, auth.getName()); }

    private long staffId(Jwt jwt) { Object v=jwt.getClaim("staffProfileId"); if(!(v instanceof Number))v=jwt.getClaim("staffId"); if(!(v instanceof Number n))throw new IllegalArgumentException("令牌缺少收费员标识");return n.longValue(); }
    /** 模拟医保结算金额请求。 */
    record SettlementCommand(BigDecimal amount) {}
}
