package com.community.healthcare.audit.application;

/**
 * 业务模块提交给审计端口的不可变事件命令。
 *
 * <p>命令只携带审计所需最小信息；{@code detailsJson} 不得包含密码、令牌、完整证件号等敏感值。</p>
 *
 * @param actor 操作者账号
 * @param actorRole 操作者在本次操作中的角色
 * @param action 稳定的动作代码
 * @param resourceType 目标资源类型
 * @param resourceId 目标资源标识
 * @param outcome 操作结果
 * @param purpose 操作目的
 * @param detailsJson 可选的结构化补充信息
 * @param correlationId 可选的跨服务关联标识
 */
public record AuditEventCommand(String actor, String actorRole, String action,
                                String resourceType, String resourceId,
                                String outcome, String purpose, String detailsJson,
                                String correlationId) {
}
