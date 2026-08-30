package com.community.healthcare.audit.application;

/**
 * 跨模块审计写入端口。
 *
 * <p>调用方依赖此接口而非具体数据库实体，使审计存储可以独立演进。</p>
 */
public interface AuditTrail {
    /**
     * 追加一条只写审计事件。
     *
     * @param command 已完成脱敏的审计命令
     */
    void append(AuditEventCommand command);
}
