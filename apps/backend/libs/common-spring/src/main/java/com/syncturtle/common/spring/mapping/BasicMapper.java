package com.syncturtle.common.spring.mapping;

import java.util.List;
import java.util.Objects;

import org.modelmapper.ModelMapper;

import com.syncturtle.common.web.dto.response.CursorIdentifiable;
import com.syncturtle.common.web.dto.response.CursorPageResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BasicMapper {

    private final ModelMapper modelMapper;
    private final CursorCodec cursorCodec;

    public BasicMapper(ModelMapper modelMapper, CursorCodec cursorCodec) {
        this.modelMapper = modelMapper;
        this.cursorCodec = cursorCodec;
    }

    public <T, S> S convertToResponse(T data, Class<S> type) {
        return modelMapper.map(data, type);
    }

    public <T, S> List<S> convertToResponseList(List<T> lists, Class<S> type) {
        if (lists == null || lists.isEmpty()) {
            return List.of();
        }

        return lists.stream()
                .filter(Objects::nonNull)
                .map(item -> convertToResponse(item, type))
                .toList();
    }

    public <T, S extends CursorIdentifiable> CursorPageResponse<S> convertToCursorPageResponse(
            List<T> items, int perPage, Class<S> type) {
        int safePerPage = Math.max(1, perPage);
        // 1: check if it has next
        boolean hasNext = items != null && items.size() > safePerPage;
        // 1: convert to desired class type
        List<S> mapped = convertToResponseList(items, type);

        List<S> pageResults = hasNext ? mapped.subList(0, safePerPage) : mapped;

        String nextCursor = null;
        if (hasNext && !pageResults.isEmpty()) {
            S last = pageResults.get(pageResults.size() - 1);
            nextCursor = cursorCodec.encode(last.getId(), last.getCreatedAt());
        }

        CursorPageResponse<S> response = new CursorPageResponse<>();
        response.setResults(pageResults);
        ;
        response.setCount(pageResults.size());
        response.setPerPage(safePerPage);
        response.setNextPageResults(hasNext);
        response.setNextCursor(nextCursor);

        // forward-only cursor paging for now
        response.setPrevPageResults(false);
        response.setPrevCursor(null);

        response.setTotalResults(0L);
        response.setTotalPages(0);
        return response;
    }

}
