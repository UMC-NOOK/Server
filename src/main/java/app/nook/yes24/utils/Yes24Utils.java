package app.nook.yes24.utils;

import app.nook.yes24.dto.Yes24ResponseDto;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

// YES24 응답 필터와 카테고리 매핑. 상품 응답에 카테고리 코드가 없어 goodsSortNm의 2단계 분류 이름으로 판별한다
@Slf4j
public final class Yes24Utils {

    private static final String DOMESTIC_PREFIX = "국내도서-";
    private static final Pattern ISBN13_PATTERN = Pattern.compile("\\d{13}");

    // YES24 2단계 분류명 -> NOOK 서비스 카테고리명 (옛 분류명 포함)
    private static final Map<String, String> SERVICE_CATEGORY_BY_NAME = Map.ofEntries(
            Map.entry("가정 살림", "가정/요리/뷰티"),
            Map.entry("가정과생활", "가정/요리/뷰티"),
            Map.entry("건강 취미", "건강/취미/레저"),
            Map.entry("건강과취미", "건강/취미/레저"),
            Map.entry("경제 경영", "경제경영"),
            Map.entry("자연과학", "과학"),
            Map.entry("자연과과학", "과학"),
            Map.entry("대학교재", "전문서적"),
            Map.entry("만화/라이트노벨", "만화"),
            Map.entry("사회 정치", "사회과학"),
            Map.entry("소설/시/희곡", "소설/시/희곡"),
            Map.entry("어린이", "어린이"),
            Map.entry("에세이", "에세이"),
            Map.entry("인물", "에세이"),
            Map.entry("여행", "여행"),
            Map.entry("역사", "역사"),
            Map.entry("예술", "예술/문화"),
            Map.entry("국어 외국어 사전", "외국어"),
            Map.entry("유아", "유아"),
            Map.entry("인문", "인문학"),
            Map.entry("자기계발", "자기계발"),
            Map.entry("자기관리", "자기계발"),
            Map.entry("종교", "종교/역학"),
            Map.entry("청소년", "청소년"),
            Map.entry("IT 모바일", "IT")
    );

    // 노출하지 않는 분류 (매핑 누락 로그 대상에서 제외)
    private static final Set<String> HIDDEN_NAMES = Set.of(
            "수험서 자격증", "전집", "중고전집", "잡지",
            "초등참고서", "중등참고서", "고등참고서", "초등학습서", "중고등학습서"
    );

    // 3단계까지 오는 분류 중 제외 대상
    private static final Set<String> HIDDEN_SUB_CATEGORIES = Set.of("IT 모바일-컴퓨터수험서");

    private Yes24Utils() {}

    // "국내도서-경제 경영" -> "경제경영". 국내도서가 아니거나 노출하지 않는 분류면 empty
    public static Optional<String> resolveServiceCategory(String goodsSortNm) {
        if (goodsSortNm == null || !goodsSortNm.startsWith(DOMESTIC_PREFIX)) {
            return Optional.empty();
        }
        String path = goodsSortNm.substring(DOMESTIC_PREFIX.length()).trim();
        if (HIDDEN_SUB_CATEGORIES.contains(path)) {
            return Optional.empty();
        }
        String name = path.split("-", 2)[0].trim();
        String serviceCategory = SERVICE_CATEGORY_BY_NAME.get(name);
        if (serviceCategory == null && !HIDDEN_NAMES.contains(name)) {
            log.warn("[YES24_UNKNOWN_CATEGORY] goodsSortNm='{}'", goodsSortNm);
        }
        return Optional.ofNullable(serviceCategory);
    }

    public static boolean isValid(Yes24ResponseDto.Yes24Item item) {
        return item != null
                && item.getIsbn13() != null && ISBN13_PATTERN.matcher(item.getIsbn13()).matches() // 세트 상품 등 ISBN 없는 상품 제외
                && "N".equals(item.getAdultYn()) // 성인 상품 제외
                && resolveServiceCategory(item.getGoodsSortNm()).isPresent(); // 국내도서·카테고리 필터
    }
}
