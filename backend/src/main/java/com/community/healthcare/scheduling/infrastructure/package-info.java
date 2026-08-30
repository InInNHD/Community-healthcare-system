/**
 * 实现排班、预约、接诊编排及其 HTTP 接口和 JPA 存储。
 *
 * <p>本包把领域规则落入事务边界，并将并发占用和幂等冲突转换为明确的业务响应。</p>
 */
package com.community.healthcare.scheduling.infrastructure;
