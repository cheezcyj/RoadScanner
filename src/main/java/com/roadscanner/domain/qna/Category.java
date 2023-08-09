package com.roadscanner.domain.qna;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {
    NOTICE("공지", 10),
    ANSWERED("답변완료", 20),
    WAITING("답변대기", 30);

    private final String label;
    private final int value;

    public static Category fromLabel(String label) {
        for (Category category : Category.values()) {
            if (category.getLabel().equalsIgnoreCase(label)) {
                return category;
            }
        }
        throw new IllegalArgumentException("라벨에러 : " + label);
    }
}
