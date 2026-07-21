package com.syncturtle.services.user.controller.dev;

import java.util.Objects;

public final class DevErrorStackFrame {

    private final String className;
    private final String methodName;
    private final String fileName;
    private final int lineNumber;
    private final String displayName;
    private final String location;
    private final boolean applicationFrame;

    public DevErrorStackFrame(
            String className,
            String methodName,
            String fileName,
            int lineNumber,
            boolean applicationFrame) {
        this.className = Objects.requireNonNullElse(className, "UnknownClass");
        this.methodName = Objects.requireNonNullElse(methodName, "unknownMethod");
        this.fileName = Objects.requireNonNullElse(fileName, "Unknown Source");
        this.lineNumber = lineNumber;
        this.applicationFrame = applicationFrame;
        this.displayName = this.className + "." + this.methodName + "()";
        this.location = this.fileName + ":" + this.lineNumber;
    }

    public static DevErrorStackFrame from(StackTraceElement frame, String basePackage) {
        Objects.requireNonNull(frame, "stack trace frame is required");

        String className = frame.getClassName();
        boolean applicationFrame = basePackage != null
                && !basePackage.isBlank()
                && className.startsWith(basePackage);

        return new DevErrorStackFrame(
                className,
                frame.getMethodName(),
                frame.getFileName(),
                frame.getLineNumber(),
                applicationFrame);
    }

    public String getClassName() {
        return this.className;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public String getFileName() {
        return this.fileName;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getLocation() {
        return this.location;
    }

    public boolean isApplicationFrame() {
        return this.applicationFrame;
    }
}