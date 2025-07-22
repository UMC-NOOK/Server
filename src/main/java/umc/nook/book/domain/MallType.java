package umc.nook.book.domain;

import lombok.Getter;

@Getter
public enum MallType {
    BOOK("국내도서"),
    FOREIGN("외국도서"),
    EBOOK("전자책");

    private final String displayName;

    MallType(String displayName) {
        this.displayName = displayName;
    }

    public static MallType fromDisplayName(String name) {
        for (MallType type : MallType.values()) {
            if (type.displayName.equals(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown mallType: " + name);
    }
}

