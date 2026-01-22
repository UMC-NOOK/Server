package app.nook.book.entity;

import java.util.Arrays;

public enum MallType {
    BOOK("국내도서"),
    FOREIGN("외국도서"),
    EBOOK("전자책"),
    ETC("기타");

    private final String displayName;

    MallType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static MallType fromDisplayName(String name) {
        if (name == null || name.isBlank()) {
            return ETC;
        }

        return Arrays.stream(values())
                .filter(type -> type.displayName.equals(name.trim()))
                .findFirst()
                .orElse(ETC); // '기타'로 처리
    }
}
