package com.roadscanner.domain.qna;

/** Defines the disjoint visibility and workflow categories stored in QUESTION.category. */
public final class QuestionCategory {
    public static final int NOTICE = 10;
    public static final int INQUIRY_ANSWERED = 20;
    public static final int INQUIRY_WAITING = 30;
    public static final int BOARD_POST = 40;

    private QuestionCategory() {
    }

    public static boolean isBoard(Integer category) {
        return category != null && (category == NOTICE || category == BOARD_POST);
    }

    public static boolean isInquiry(Integer category) {
        return category != null
                && (category == INQUIRY_ANSWERED || category == INQUIRY_WAITING);
    }

    public static boolean isSupported(Integer category) {
        return isBoard(category) || isInquiry(category);
    }

    public static boolean isCreatable(Integer category) {
        return category != null
                && (category == NOTICE || category == INQUIRY_WAITING || category == BOARD_POST);
    }
}
