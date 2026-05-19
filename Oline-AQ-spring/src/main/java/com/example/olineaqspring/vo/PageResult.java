package com.example.olineaqspring.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageResult<T> {
    private List<T> list;
    private long total;
    private int page;
    private int pageSize;

    public long getTotalPages() {
        return pageSize > 0 ? (total + pageSize - 1) / pageSize : 0;
    }
}
