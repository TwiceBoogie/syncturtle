package com.syncturtle.common.spring.mapping;

import java.util.ArrayList;
import java.util.List;

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
        return lists.contains(null) ? new ArrayList<>()
                : lists.stream().map(list -> convertToResponse(list, type)).toList();
    }
}
