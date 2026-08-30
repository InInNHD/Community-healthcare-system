package com.community.healthcare.shared.api;

import org.springframework.data.domain.Page;
import java.util.List;

/**
 * 前后端统一使用的零基页码分页响应。
 *
 * @param items 当前页数据
 * @param total 符合条件的总记录数
 * @param page 当前页码，从 0 开始
 * @param size 请求的每页大小
 * @param totalPages 总页数
 * @param <T> 列表元素类型
 */
public record PageResponse<T>(List<T> items, long total, int page, int size, int totalPages) {
    /**
     * 将 Spring Data 分页结果转换为稳定的 API 结构。
     *
     * @param source JPA 查询返回的分页结果
     * @return 保留分页元数据的 API 响应
     * @param <T> 列表元素类型
     */
    public static <T> PageResponse<T> of(Page<T> source) {
        return new PageResponse<>(source.getContent(), source.getTotalElements(), source.getNumber(),
                source.getSize(), source.getTotalPages());
    }
}
