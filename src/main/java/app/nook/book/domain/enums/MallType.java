package app.nook.book.domain.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum MallType {
    BOOK("국내도서"),
    EBOOK("전자책"),
    ETC("기타");

    private final String displayName;

    MallType(String displayName) {
        this.displayName = displayName;
    }

    public static Optional<MallType> fromDisplayName(String name) {
        return Arrays.stream(values())
                .filter(type -> type.displayName.equals(name.trim()))
                .findFirst();
    }
}
