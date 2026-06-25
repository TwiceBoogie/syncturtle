package com.syncturtle.services.workspace.model.support;

import com.syncturtle.services.workspace.type.WorkspaceRole;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class WorkspaceRoleConverter implements AttributeConverter<WorkspaceRole, Integer> {

    @Override
    public Integer convertToDatabaseColumn(WorkspaceRole attribute) {
        return (attribute == null) ? null : attribute.code;
    }

    @Override
    public WorkspaceRole convertToEntityAttribute(Integer dbData) {
        return (dbData == null) ? null : WorkspaceRole.from(dbData);
    }

}
