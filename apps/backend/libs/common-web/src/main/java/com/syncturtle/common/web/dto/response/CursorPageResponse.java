package com.syncturtle.common.web.dto.response;

import java.util.List;

import lombok.Data;

@Data
public final class CursorPageResponse<T> {
    private int count;
    private String nextCursor;
    private boolean nextPageResults;
    private String prevCursor;
    private boolean prevPageResults;
    private int totalPages;
    private Integer perPage;
    private long totalResults;
    private List<T> results;
}
