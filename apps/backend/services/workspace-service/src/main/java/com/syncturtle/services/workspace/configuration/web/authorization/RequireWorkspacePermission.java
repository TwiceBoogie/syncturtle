package com.syncturtle.services.workspace.configuration.web.authorization;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.syncturtle.common.contracts.workspace.type.WorkspaceRoleCodes;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface RequireWorkspacePermission {
    int minRole() default WorkspaceRoleCodes.GUEST;
}
