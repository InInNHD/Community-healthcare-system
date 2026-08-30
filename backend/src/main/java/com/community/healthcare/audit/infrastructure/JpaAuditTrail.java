package com.community.healthcare.audit.infrastructure;

import com.community.healthcare.audit.application.AuditEventCommand;
import com.community.healthcare.audit.application.AuditTrail;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 使用独立事务持久化审计事件的 JPA 适配器。
 *
 * <p>{@link Propagation#REQUIRES_NEW} 使审计写入拥有独立提交边界；调用方不能据此假定原业务事务成功，
 * 因此事件的 {@code outcome} 必须由调用场景准确提供。</p>
 */
@Component
class JpaAuditTrail implements AuditTrail {
    private final AuditEventRepository events;

    JpaAuditTrail(AuditEventRepository events) {
        this.events = events;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void append(AuditEventCommand command) {
        events.save(new AuditEventEntity(command));
    }
}

/** 将审计命令映射为追加后不再更新的数据库记录。 */
@Entity
@Table(name = "audit_event")
class AuditEventEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, updatable = false) private Instant occurredAt;
    @Column(nullable = false, updatable = false, length = 128) private String actor;
    @Column(updatable = false, length = 32) private String actorRole;
    @Column(nullable = false, updatable = false, length = 64) private String action;
    @Column(nullable = false, updatable = false, length = 64) private String resourceType;
    @Column(updatable = false, length = 128) private String resourceId;
    @Column(nullable = false, updatable = false, length = 32) private String outcome;
    @Column(updatable = false, length = 255) private String purpose;
    @Column(updatable = false, columnDefinition = "TEXT") private String detailsJson;
    @Column(updatable = false, length = 64) private String correlationId;

    protected AuditEventEntity() {}

    AuditEventEntity(AuditEventCommand command) {
        occurredAt = Instant.now();
        actor = command.actor(); actorRole = command.actorRole(); action = command.action();
        resourceType = command.resourceType(); resourceId = command.resourceId();
        outcome = command.outcome(); purpose = command.purpose(); detailsJson = command.detailsJson();
        correlationId = command.correlationId();
    }
}

/** 审计事件的内部 JPA 仓储，不对其他业务模块开放。 */
interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {
}
