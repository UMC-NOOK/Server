package umc.nook.book.utils;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class BookFilterUtils {

    // 책의 카테고리가 도서 정책에 포함되는지 여부(true -> 포함 O, false -> 포함 x)
    public static boolean isBookIncluded(String fullCategoryName) {
        // 1depth 제외 카테고리 맵
        Map<String, Set<String>> excluded1Depth = Map.of(
                "국내도서", Set.of("고등학교참고서", "수험서/자격증", "잡지", "중학교참고서", "초등학교참고서", "Gift"),
                "외국도서", Set.of("게임/토이", "달력/다이어리/연감", "문구/비도서", "수험서", "해외잡지"),
                "eBook", Set.of("19+", "가격대별 eBook", "고등학교참고서", "수험서/자격증", "잡지", "중고등참고서", "중학교참고서", "초등참고서", "Gift")
        );

        // 2depth 제외 조건 맵 (문자열 포함 여부)
        Map<String, Map<String, List<String>>> excluded2DepthContains = Map.of(
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
        if (fullCategoryName != null && !fullCategoryName.isBlank()) {
            String[] parts = fullCategoryName.split(">");
            if (parts.length >= 2) {
                String mallType = parts[0].trim(); // 몰타입 추출
                String firstDepth = parts[1].trim(); // 1depth 추출

                // 1depth 제외 체크
                Set<String> excludes1 = excluded1Depth.getOrDefault(mallType, Set.of());
                if (excludes1.contains(firstDepth)) {
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
}
