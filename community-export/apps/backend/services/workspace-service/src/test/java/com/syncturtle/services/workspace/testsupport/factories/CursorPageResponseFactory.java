package com.syncturtle.services.workspace.testsupport.factories;

import java.util.List;

import com.syncturtle.common.web.pagination.CursorPageResponse;

public final class CursorPageResponseFactory {

    private CursorPageResponseFactory() {
        throw new AssertionError("No instance.");
    }

    public static <T> CursorPageResponse<T> page(List<T> results, int perPage) {
        CursorPageResponse<T> response = new CursorPageResponse<>();
        response.setResults(results);
        response.setCount(results == null ? 0 : results.size());
        response.setPerPage(perPage);

        response.setNextCursor(null);
        response.setNextPageResults(false);
        response.setPrevCursor(null);
        response.setPrevPageResults(false);
        response.setTotalPages(1);
        response.setTotalResults(response.getCount());

        return response;
    }

    public static <T> CursorPageResponse<T> pageWithNext(List<T> results, int perPage, String nextCursor) {
        CursorPageResponse<T> response = page(results, perPage);
        response.setNextCursor(nextCursor);
        response.setNextPageResults(true);
        return response;
    }

    public static <T> CursorPageResponse<T> pageWithPrev(List<T> results, int perPage, String prevCursor) {
        CursorPageResponse<T> response = page(results, perPage);
        response.setPrevCursor(prevCursor);
        response.setPrevPageResults(true);
        return response;
    }

}
