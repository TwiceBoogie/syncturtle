package com.syncturtle.services.user.controller.dev;

import java.util.List;
import java.util.Objects;

import lombok.Getter;

@Getter
public final class SourceSnippet {

    private final String sourceFile;
    private final Integer sourceLine;
    private final List<SourceLine> lines;

    private SourceSnippet(String sourceFile, Integer sourceLine, List<SourceLine> lines) {
        this.sourceFile = sourceFile;
        this.sourceLine = sourceLine;
        this.lines = List.copyOf(Objects.requireNonNull(lines, "source lines are required"));
    }

    public static SourceSnippet empty() {
        return new SourceSnippet(null, null, List.of());
    }

    public static SourceSnippet empty(String sourceFile, int sourceLine) {
        return new SourceSnippet(sourceFile, sourceLine > 0 ? sourceLine : null, List.of());
    }

    public static SourceSnippet of(String sourceFile, int sourceLine, List<SourceLine> lines) {
        return new SourceSnippet(sourceFile, sourceLine, lines);
    }

}
