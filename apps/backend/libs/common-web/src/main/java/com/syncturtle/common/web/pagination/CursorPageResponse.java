package com.syncturtle.common.web.pagination;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CursorPageResponse<T> {
    String nextCursor;
    String prevCursor;
    boolean nextPageResults;
    boolean prevPageResults;
    int count;
    int totalPages;
    Integer perPage;
    long totalResults;
    List<T> results;
}
