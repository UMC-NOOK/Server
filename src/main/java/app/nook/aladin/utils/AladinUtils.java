package app.nook.aladin.utils;

import app.nook.aladin.dto.AladinResponseDto;
import app.nook.aladin.exception.AladinErrorCode;
import app.nook.book.domain.enums.BookCategory;
import app.nook.book.domain.enums.MallType;
import app.nook.global.exception.CustomException;

import java.util.List;
import java.util.regex.Pattern;

public final class AladinUtils {

    private static final List<String> ALLOWED_MALL_TYPES = List.of("BOOK", "EBOOK");
    private static final Pattern ISBN13_PATTERN = Pattern.compile("\\d{13}");

    private AladinUtils() {}

    // 문자열 정규화 (null 체크 및 앞뒤 공백 제거)
    public static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    // BOOK, EBOOK을 몰타입 형으로
    public static MallType extractMallType(String code) {
        try {
            return MallType.fromCode(code);
        } catch (IllegalArgumentException e) {
            throw new CustomException(AladinErrorCode.ALADIN_INVALID_MALLTYPE);
        }
    }

    // "국내도서>소설..." -> "소설" (카테고리명 추출)
    public static String extractCategoryName(String rawCategory) {
        if (rawCategory == null || rawCategory.isEmpty()) {
            return "";
        }
        String[] parts = rawCategory.split(">");
        return normalize(parts.length > 1 ? parts[1] : "");
    }

    public static boolean isValid(AladinResponseDto.AladinItem item) {
        return item != null &&
                item.getIsbn13() != null && ISBN13_PATTERN.matcher(item.getIsbn13()).matches() && // 세트 상품 등 ISBN 없는 상품은 상세 진입 불가
                !item.isAdult() && // 19금 필터링
                item.getMallType() != null &&
                ALLOWED_MALL_TYPES.contains(item.getMallType().toUpperCase()) && // 몰타입 필터링
                BookCategory.match(extractCategoryName(item.getCategoryName())).isPresent(); // 카테고리 필터링
    }
}
