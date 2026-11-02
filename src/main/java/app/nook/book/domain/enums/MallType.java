package app.nook.book.domain.enums;

import lombok.Getter;

// 전자책은 서비스에서 제외되어 국내 종이책(BOOK)만 다룬다
@Getter
public enum MallType {
    BOOK("국내도서");

    private final String displayName;

    MallType(String displayName) {
        this.displayName = displayName;
    }
}
