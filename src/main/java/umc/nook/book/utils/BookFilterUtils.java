package umc.nook.book.utils;

import org.springframework.stereotype.Component;
import umc.nook.aladin.dto.AladinResponseDTO;
import umc.nook.book.domain.Category;
import umc.nook.book.domain.MallType;
import umc.nook.book.init.CategoryInitializer;

import java.util.*;

public class BookFilterUtils {
    // 2depth 제외 조건 맵 (문자열 포함 여부)
    static Map<String, Map<String, List<String>>> excluded2DepthContains = Map.of(
            "국내도서", Map.of(
                    "달력/기타", List.of("달력", "다이어리", "가계부")
            ),
            "외국도서", Map.of(
                    "독일 도서", List.of("CD/CD-ROM/DVD"),
                    "어린이", List.of("캐릭터"),
                    "일본 도서", List.of("애니메이션 굿즈", "엔터테인먼트", "잡지", "지브리 작품전", "캘린더", "CD/DVD"),
                    "중국 도서", List.of("CD/DVD/VCD")
            )
    );
    // 책의 카테고리가 도서 정책에 포함되는지 여부(true -> 포함 O, false -> 포함 x)
    public static boolean isBookIncluded(String fullCategoryName) {
        Map<String, Set<String>> allowed1Depth = new HashMap<>();

        for (Category category : CategoryInitializer.categories) {
            allowed1Depth
                    .computeIfAbsent(category.getMallType().name(), k -> new HashSet<>())
                    .add(category.getCategoryName());
        }
        if (fullCategoryName != null && !fullCategoryName.isBlank()) {
            String[] parts = fullCategoryName.split(">");
            if (parts.length >= 2) {
                String mallType = parts[0].trim(); // 몰타입 추출
                String firstDepth = parts[1].trim(); // 1depth 추출
                String mallTypeKey;

                try {
                    mallTypeKey = MallType.fromDisplayName(mallType).name();
                } catch (IllegalArgumentException e) {
                    return false;
                }

                // 1depth 제외 체크
                Set<String> allowSet = allowed1Depth.getOrDefault(mallTypeKey, Set.of());
                if (!allowSet.contains(firstDepth)) {
                    return false;
                }

                // 2depth 문자열 포함 제외 체크
                if (parts.length >= 3) {
                    String secondDepth = parts[2].trim(); // 2depth 추출
                    if (excluded2DepthContains.containsKey(mallType)) {
                        Map<String, List<String>> firstDepthMap = excluded2DepthContains.get(mallType);
                        if (firstDepthMap.containsKey(firstDepth)) {
                            for (String keyword : firstDepthMap.get(firstDepth)) {
                                if (secondDepth.contains(keyword)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public static boolean isValidMallType(String mallType) {
        Set<String> VALID_MALL_TYPES = Set.of("BOOK", "FOREIGN", "EBOOK");
        return mallType != null && VALID_MALL_TYPES.contains(mallType.toUpperCase());
    }
    public static boolean isValidBook(AladinResponseDTO.BookDetailDTO item) {
        if (item.getIsbn13() == null || item.getIsbn13().isBlank() || item.getIsbn13().length() != 13) return false;
        if (item.getCategoryName() == null || item.getCategoryName().isBlank()) return false;
        if (!isValidMallType(item.getMallType())) return false;

        // depth(>) 최소 2개 이상 체크
        String[] parts = item.getCategoryName().split(">");
        if (parts.length < 2) return false;

        return isBookIncluded(item.getCategoryName());
    }
}
