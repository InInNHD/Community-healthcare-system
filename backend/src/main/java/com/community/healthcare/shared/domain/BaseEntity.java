package com.community.healthcare.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/**
 * 为 JPA 领域实体提供创建/更新时间、乐观锁版本和统一软删除标记。
 *
 * <p>{@code version} 用于发现并发覆盖；软删除只记录删除时间和操作者，具体查询仍必须明确排除
 * {@code deletedAt} 非空的记录。</p>
 */
@MappedSuperclass
public abstract class BaseEntity {
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @Version
    private long version;
    @Column
    private LocalDateTime deletedAt;
    @Column(length = 128)
    private String deletedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public String getDeletedBy() { return deletedBy; }

    /**
     * 幂等地标记实体为已删除，并保留执行者用于审计。
     *
     * @param actor 发起删除的账号；空值或空白值统一记为 {@code system}
     */
    protected void markDeleted(String actor) {
        if (deletedAt != null) return;
        deletedAt = LocalDateTime.now();
        deletedBy = actor == null || actor.isBlank() ? "system" : actor;
    }
}
