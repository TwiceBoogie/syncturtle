package com.syncturtle.services.instance.model.support;

import java.util.Optional;

import com.syncturtle.services.instance.type.InstanceAdminRole;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class InstanceRoleConverter implements AttributeConverter<InstanceAdminRole, Integer> {

    @Override
    public Integer convertToDatabaseColumn(InstanceAdminRole attribute) {
        if (attribute == null) {
            return null;
        }

        return attribute.getCode();
    }

    @Override
    public InstanceAdminRole convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }

        Optional<InstanceAdminRole> role = InstanceAdminRole.fromCode(dbData);

        return role.isPresent() ? role.get() : null;
    }

}
