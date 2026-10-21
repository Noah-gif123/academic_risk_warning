package com.example.academic_risk_warning.common;

import lombok.Data;

import java.util.List;

/** 简易分页结果（本阶段数据量小，采用内存切片；后续可换成 MyBatis-Plus 分页插件） */
@Data
public class PageResult<T> {
    private long total;
    private int page;
    private int size;
    private List<T> records;

    public static <T> PageResult<T> of(List<T> all, int page, int size) {
        int p = Math.max(page, 1);
        int s = size <= 0 ? 20 : size;
        int from = Math.min((p - 1) * s, all.size());
        int to = Math.min(from + s, all.size());
        PageResult<T> r = new PageResult<>();
        r.total = all.size();
        r.page = p;
        r.size = s;
        r.records = all.subList(from, to);
        return r;
    }
}
