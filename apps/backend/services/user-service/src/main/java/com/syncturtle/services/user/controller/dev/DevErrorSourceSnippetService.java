package com.syncturtle.services.user.controller.dev;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("local")
public final class DevErrorSourceSnippetService {

    private static final List<String> FRAME_PREFIXES_TO_SKIP = List.of(
            "java.",
            "jdk.",
            "sun.",
            "jakarta.",
            "javax.",
            "org.springframework.",
            "org.apache.catalina.",
            "org.apache.tomcat.");

    private final String basePackage;
    private final List<Path> sourceRoots;

    public DevErrorSourceSnippetService(Environment environment) {
        Objects.requireNonNull(environment, "environment is required");

        this.basePackage = environment.getProperty("app.dev-error.base-package", "");

        String sourceRootsCsv = environment.getProperty("app.dev-error.source-roots",
                "src/main/java,src/test/java,src/main/resources,src/test/resources");

        Path projectRoot = Path.of("").toAbsolutePath().normalize();

        this.sourceRoots = Arrays.stream(sourceRootsCsv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(projectRoot::resolve)
                .map(Path::normalize)
                .toList();
    }

    public SourceSnippet findSourceSnippet(Throwable error) {
        if (error == null) {
            return SourceSnippet.empty();
        }

        Throwable mostSpecificCause = mostSpecificCause(error);
        StackTraceElement frame = findApplicationFrame(mostSpecificCause.getStackTrace());

        if (frame == null) {
            return SourceSnippet.empty();
        }

        Path sourceFile = resolveSourceFile(frame);

        if (sourceFile == null || !Files.isRegularFile(sourceFile)) {
            return SourceSnippet.empty(frame.getFileName(), frame.getLineNumber());
        }

        try {
            return readSourceSnippet(sourceFile, frame.getLineNumber());
        } catch (IOException exception) {
            return SourceSnippet.empty(displayPath(sourceFile), frame.getLineNumber());
        }
    }

    private StackTraceElement findApplicationFrame(StackTraceElement[] frames) {
        if (frames == null || frames.length == 0) {
            return null;
        }

        if (StringUtils.hasText(this.basePackage)) {
            for (StackTraceElement frame : frames) {
                if (frame.getClassName().startsWith(this.basePackage)) {
                    return frame;
                }
            }
        }

        for (StackTraceElement frame : frames) {
            if (!shouldSkip(frame.getClassName())) {
                return frame;
            }
        }

        return frames[0];
    }

    private Path resolveSourceFile(StackTraceElement frame) {
        String className = frame.getClassName();
        int innerClassIndex = className.indexOf('$');

        if (innerClassIndex >= 0) {
            className = className.substring(0, innerClassIndex);
        }

        String relativeJavaPath = className.replace('.', '/') + ".java";

        for (Path sourceRoot : this.sourceRoots) {
            Path candidate = sourceRoot.resolve(relativeJavaPath).normalize();

            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private SourceSnippet readSourceSnippet(Path sourceFile, int activeLine) throws IOException {
        List<String> fileLines = Files.readAllLines(sourceFile, StandardCharsets.UTF_8);

        if (activeLine < 1 || activeLine > fileLines.size()) {
            return SourceSnippet.empty(displayPath(sourceFile), activeLine);
        }

        int start = Math.max(1, activeLine - 6);
        int end = Math.min(fileLines.size(), activeLine + 6);

        List<SourceLine> sourceLines = new ArrayList<>();

        for (int lineNumber = start; lineNumber <= end; lineNumber++) {
            String text = fileLines.get(lineNumber - 1);
            boolean active = lineNumber == activeLine;

            sourceLines.add(new SourceLine(lineNumber, text, active));
        }

        return SourceSnippet.of(displayPath(sourceFile), activeLine, sourceLines);
    }

    private static Throwable mostSpecificCause(Throwable error) {
        Throwable result = error;

        while (result.getCause() != null && result.getCause() != result) {
            result = result.getCause();
        }

        return result;
    }

    private static boolean shouldSkip(String className) {
        if (!StringUtils.hasText(className)) {
            return true;
        }

        for (String prefix : FRAME_PREFIXES_TO_SKIP) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    private static String displayPath(Path path) {
        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        Path normalizedPath = path.toAbsolutePath().normalize();

        try {
            return projectRoot.relativize(normalizedPath).toString();
        } catch (IllegalArgumentException exception) {
            return normalizedPath.toString();
        }
    }

}
