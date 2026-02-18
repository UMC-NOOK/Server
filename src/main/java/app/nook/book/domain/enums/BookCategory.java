package app.nook.book.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum BookCategory {
    // 형식: (DB 저장명, BOOK_ID, EBOOK_ID, [매핑_Alias_리스트])

    // 1. 단순 1:1 매핑
    // TODO: 필터링 기능 리팩토링 예정
    HOME_COOKING("가정/요리/뷰티", 1230, 38409, List.of("가정/요리/뷰티")),
    HEALTH("건강/취미/레저", 55890, 56388, List.of("건강/취미/레저")),
    ECONOMY("경제경영", 170, 38398, List.of("경제경영")),
    CLASSIC("고전", 2105, 38414, List.of("고전", "고등학교참고서")),
    SCIENCE("과학", 987, 38405, List.of("과학")),
    COMICS("만화", 2551, 38416, List.of("만화")),
    SOCIAL("사회과학", 798, 38404, List.of("사회과학")),
    CHILDREN("어린이", 1108, 38406, List.of("어린이")),
    ESSAY("에세이", 55889, 56387, List.of("에세이")),
    TRAVEL("여행", 1196, 38408, List.of("여행")),
    HISTORY("역사", 74, 38397, List.of("역사")),
    FOREIGN_LANG("외국어", 1322, 38411, List.of("외국어")),
    TODDLER("유아", 13789, 38424, List.of("유아")),
    HUMANITIES("인문학", 656, 38403, List.of("인문학")),
    SELF_HELP("자기계발", 336, 38400, List.of("자기계발")),
    RELIGION("종교/역학", 1237, 38410, List.of("종교/역학")),
    PARENTING("좋은부모", 2030, 38413, List.of("좋은부모")),
    TEENAGER("청소년", 1137, 38407, List.of("청소년")),
    COMPUTER("컴퓨터/모바일", 351, 38401, List.of("컴퓨터/모바일")),

    // 2. 이름 변경 (Rename)
    PROFESSIONAL("전문서적", 8257, 38422, List.of("전문서적", "대학교재/전문서적")),
    ART("예술/문화", 517, 38402, List.of("예술/문화", "예술/대중문화")),

    // 3. 다대일 통합 (소설/시/희곡)
    // 알라딘에서 '라이트 노벨' 등 세부 장르로 와도, DB엔 '소설/시/희곡'으로 저장하고 ID는 대표 ID(1, 38396) 로 통일
    NOVEL("소설/시/희곡", 1, 38396, List.of(
            "소설/시/희곡",
            "라이트 노벨",
            "로맨스",
            "판타지/무협",
            "BL"
    ));

    private final String dbName;
    private final int bookId;  // 알라딘 국내도서 CID
    private final int ebookId; // 알라딘 전자책 CID
    private final List<String> aliases;

    private static final Map<String, BookCategory> ALIAS_MAP = new HashMap<>();

    static {
        for (BookCategory category : values()) {
            for (String alias : category.aliases) {
                ALIAS_MAP.put(alias, category);
            }
        }
    }

    public static Optional<BookCategory> match(String rawCategoryName) {
        if (rawCategoryName == null) return Optional.empty();
        return Optional.ofNullable(ALIAS_MAP.get(rawCategoryName));
    }

    // DB에 저장된 이름으로 Enum 찾기
    public static Optional<BookCategory> findByDbName(String dbName) {
        for (BookCategory category : values()) {
            if (category.dbName.equals(dbName)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }
}
