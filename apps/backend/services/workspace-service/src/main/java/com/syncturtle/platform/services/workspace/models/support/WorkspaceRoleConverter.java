package com.syncturtle.platform.services.workspace.models.support;

import com.syncturtle.platform.services.workspace.enums.WorkspaceRole;

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
