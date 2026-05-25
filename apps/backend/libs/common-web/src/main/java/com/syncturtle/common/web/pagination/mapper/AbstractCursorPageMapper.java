package com.syncturtle.common.web.pagination.mapper;

import java.util.List;

import org.springframework.util.Assert;

import com.syncturtle.common.web.pagination.CursorCodec;
import com.syncturtle.common.web.pagination.CursorIdentifiable;
import com.syncturtle.common.web.pagination.CursorPageResponse;

public abstract class AbstractCursorPageMapper<T, S extends CursorIdentifiable> {

    private final CursorCodec cursorCodec;

    protected AbstractCursorPageMapper(CursorCodec cursorCodec) {
        Assert.notNull(cursorCodec, "cursorCodec is required");

        this.cursorCodec = cursorCodec;
    }

    protected abstract S mapToResponse(T source);

    public CursorPageResponse<S> mapToPageResponse(List<T> sources, int perPage) {
        Assert.notNull(sources, "The items list must not be null");
        Assert.noNullElements(sources, "The items list must not contain null elements");
        Assert.isTrue(perPage > 0, "perPage must be greater than 0");

        boolean hasNext = sources.size() > perPage;

        List<T> pageSources = hasNext ? sources.subList(0, perPage) : sources;

        List<S> results = pageSources.stream()
                .map(this::mapToResponse)
                .toList();

        String nextCursor = null;
        if (hasNext && !results.isEmpty()) {
            S last = results.get(results.size() - 1);
            nextCursor = cursorCodec.encode(last.getId(), last.getCreatedAt());
        }

        return CursorPageResponse.<S>builder()
                .results(results)
                .count(results.size())
                .perPage(perPage)
                .nextPageResults(hasNext)
                .nextCursor(nextCursor)
                .prevPageResults(false)
                .prevCursor(null)
                .totalResults(0L)
                .totalPages(0)
                .build();
    }

}
