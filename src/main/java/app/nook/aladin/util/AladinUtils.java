package app.nook.aladin.util;

import app.nook.book.entity.MallType;

public final class AladinUtils {
    private AladinUtils() {}

    // 문자열 정규화 (null 체크 및 앞뒤 공백 제거)
    public static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    // "국내도서>소설..." -> "국내도서" (MallType 추출)
    public static MallType extractMallType(String rawCategory) {
        if (rawCategory == null || rawCategory.isEmpty()) {
            return MallType.BOOK;
        }
        String[] parts = rawCategory.split(">");
        String mallTypeName = normalize(parts.length > 0 ? parts[0] : "");
        return MallType.fromDisplayName(mallTypeName).orElse(MallType.BOOK);
    }

    // "국내도서>소설..." -> "소설" (카테고리명 추출)
    public static String extractCategoryName(String rawCategory) {
        if (rawCategory == null || rawCategory.isEmpty()) {
            return "";
        }
        String[] parts = rawCategory.split(">");
        return normalize(parts.length > 1 ? parts[1] : "");
    }
}
