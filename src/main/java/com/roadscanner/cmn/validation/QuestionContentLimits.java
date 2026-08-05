package com.roadscanner.cmn.validation;

/** Shared limits applied before HTML parsing and to visible editor text. */
public final class QuestionContentLimits {

    public static final int MAX_VISIBLE_LENGTH = 10_000;
    public static final int MAX_RAW_LENGTH = 262_144;

    private QuestionContentLimits() {
    }
}
