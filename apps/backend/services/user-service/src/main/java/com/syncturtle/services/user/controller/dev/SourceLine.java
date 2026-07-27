package com.syncturtle.services.user.controller.dev;

import lombok.Getter;

@Getter
public final class SourceLine {

    private final int number;
    private final String text;
    private final boolean active;

    public SourceLine(int number, String text, boolean active) {
        this.number = number;
        this.text = text;
        this.active = active;
    }

}
