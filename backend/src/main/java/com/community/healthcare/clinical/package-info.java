/**
 * 承载现阶段管理端、医护端和居民端共享的临床业务能力。
 *
 * <p>本包编排预约、健康记录、慢病档案和药品库存等既有业务；新增独立领域优先通过专门模块扩展，
 * 门户 Controller 不应绕过应用服务直接修改业务状态。</p>
 */
package com.community.healthcare.clinical;
