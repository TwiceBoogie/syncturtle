package com.syncturtle.services.workspace.model.support;

import com.syncturtle.services.workspace.type.WorkspaceRole;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class WorkspaceRoleConverter implements AttributeConverter<WorkspaceRole, Short> {

    @Override
    public Short convertToDatabaseColumn(WorkspaceRole attribute) {
        return (attribute == null) ? null : (short) attribute.code;
    }

    @Override
    public WorkspaceRole convertToEntityAttribute(Short dbData) {
        return (dbData == null) ? null : WorkspaceRole.from(dbData);
    }

}
