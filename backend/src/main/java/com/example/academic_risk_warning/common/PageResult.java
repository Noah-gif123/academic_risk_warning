package com.example.academic_risk_warning.common;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果封装
 */
public class PageResult<T> {

    private final long total;
    private final int page;
    private final int size;
    private final List<T> records;

    public PageResult(long total, int page, int size, List<T> records) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.records = records;
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(0, 1, 10, Collections.emptyList());
    }

    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public List<T> getRecords() { return records; }
}
