/**
 * 实现处方、库存和收费之间的事务性业务编排。
 *
 * <p>本包负责把领域状态变化与 JPA 持久化结合，并将并发或状态冲突转换为稳定的业务错误。</p>
 */
package com.community.healthcare.pharmacy.infrastructure;
