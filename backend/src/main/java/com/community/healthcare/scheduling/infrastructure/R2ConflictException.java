package com.community.healthcare.scheduling.infrastructure;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 表示客户端提交的业务操作与当前资源状态发生冲突。
 *
 * <p>该异常统一映射为 HTTP 409，适用于号源抢占、乐观锁版本过期和状态机前置条件
 * 不满足等可由客户端刷新后重试的场景。</p>
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class R2ConflictException extends RuntimeException {
    /** 创建携带可展示业务提示的冲突异常。 */
    public R2ConflictException(String message) { super(message); }
}
