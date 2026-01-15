package com.syncturtle.common.spring.mapping;

import java.util.List;
import java.util.Objects;

import org.modelmapper.ModelMapper;

public class BasicMapper {

    private final ModelMapper modelMapper;

    public BasicMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
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
}
