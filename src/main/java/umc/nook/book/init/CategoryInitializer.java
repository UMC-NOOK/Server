package umc.nook.book.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import umc.nook.book.domain.Category;
import umc.nook.book.domain.MallType;
import umc.nook.book.repository.CategoryRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (categoryRepository.count() == 0) {
            List<Category> categories = List.of(
                    // BOOK (국내 도서)
                    Category.of(MallType.BOOK, "가정/요리/뷰티", 1230),
                    Category.of(MallType.BOOK, "건강/취미/레저", 55890),
                    Category.of(MallType.BOOK, "경제경영", 170),
                    Category.of(MallType.BOOK, "고전", 2105),
                    Category.of(MallType.BOOK, "과학", 987),
                    Category.of(MallType.BOOK, "달력/기타", 4395),
                    Category.of(MallType.BOOK, "대학교재/전문서적", 8257),
                    Category.of(MallType.BOOK, "만화", 2551),
                    Category.of(MallType.BOOK, "사회과학", 8257),
                    Category.of(MallType.BOOK, "소설/시/희곡", 1),
                    Category.of(MallType.BOOK, "어린이", 1108),
                    Category.of(MallType.BOOK, "에세이", 55889),
                    Category.of(MallType.BOOK, "여행", 1196),
                    Category.of(MallType.BOOK, "역사", 74),
                    Category.of(MallType.BOOK, "예술/대중문화", 517),
                    Category.of(MallType.BOOK, "외국어", 1322),
                    Category.of(MallType.BOOK, "유아", 13789),
                    Category.of(MallType.BOOK, "인문학", 656),
                    Category.of(MallType.BOOK, "자기계발", 336),
                    Category.of(MallType.BOOK, "장르소설", 112011),
                    Category.of(MallType.BOOK, "전집/중고전집", 17195),
                    Category.of(MallType.BOOK, "종교/역학", 1237),
                    Category.of(MallType.BOOK, "좋은부모", 2030),
                    Category.of(MallType.BOOK, "청소년", 1137),
                    Category.of(MallType.BOOK, "컴퓨터/모바일", 351),

                    // FOREIGN (외국 도서)
                    Category.of(MallType.FOREIGN, "가정/원예/인테리어", 90831),
                    Category.of(MallType.FOREIGN, "가족/관계", 90832),
                    Category.of(MallType.FOREIGN, "건강/스포츠", 90833),
                    Category.of(MallType.FOREIGN, "건축/디자인", 90834),
                    Category.of(MallType.FOREIGN, "경제경영", 90835),
                    Category.of(MallType.FOREIGN, "공예/취미/수집", 90836),
                    Category.of(MallType.FOREIGN, "교육/자료", 90837),
                    Category.of(MallType.FOREIGN, "기술공학", 90838),
                    Category.of(MallType.FOREIGN, "기타", 25992),
                    Category.of(MallType.FOREIGN, "기타 언어권 도서", 28253),
                    Category.of(MallType.FOREIGN, "대만도서", 136215),
                    Category.of(MallType.FOREIGN, "대학교재", 30177),
                    Category.of(MallType.FOREIGN, "독일 도서", 145394),
                    Category.of(MallType.FOREIGN, "만화", 90840),
                    Category.of(MallType.FOREIGN, "법률", 90841),
                    Category.of(MallType.FOREIGN, "소설/시/희곡", 90842),
                    Category.of(MallType.FOREIGN, "스페인 도서", 28254),
                    Category.of(MallType.FOREIGN, "어린이", 106165),
                    Category.of(MallType.FOREIGN, "언어학", 90844),
                    Category.of(MallType.FOREIGN, "에세이", 90845),
                    Category.of(MallType.FOREIGN, "여행", 90846),
                    Category.of(MallType.FOREIGN, "역사", 90847),
                    Category.of(MallType.FOREIGN, "예술/대중문화", 90848),
                    Category.of(MallType.FOREIGN, "오디오북", 90849),
                    Category.of(MallType.FOREIGN, "요리", 90850),
                    Category.of(MallType.FOREIGN, "유머", 90851),
                    Category.of(MallType.FOREIGN, "의학", 90852),
                    Category.of(MallType.FOREIGN, "인문/사회", 90853),
                    Category.of(MallType.FOREIGN, "일본 도서", 28261),
                    Category.of(MallType.FOREIGN, "자기계발", 90854),
                    Category.of(MallType.FOREIGN, "자연과학", 90855),
                    Category.of(MallType.FOREIGN, "전기/자서전", 90856),
                    Category.of(MallType.FOREIGN, "종교/명상/점술", 90857),
                    Category.of(MallType.FOREIGN, "중국 도서", 28492),
                    Category.of(MallType.FOREIGN, "청소년", 90858),
                    Category.of(MallType.FOREIGN, "컴퓨터", 90859),
                    Category.of(MallType.FOREIGN, "한국관련도서", 90860),
                    Category.of(MallType.FOREIGN, "ELT/어학/사전", 90861),

                    // EBOOK (전자책)
                    Category.of(MallType.EBOOK, "가정/요리/뷰티", 38409),
                    Category.of(MallType.EBOOK, "건강/취미/레저", 56388),
                    Category.of(MallType.EBOOK, "경제경영", 38398),
                    Category.of(MallType.EBOOK, "고전", 38414),
                    Category.of(MallType.EBOOK, "과학", 38405),
                    Category.of(MallType.EBOOK, "구텐베르크 원서", 72509),
                    Category.of(MallType.EBOOK, "대학교재/전문서적", 38422),
                    Category.of(MallType.EBOOK, "라이트 노벨", 56548),
                    Category.of(MallType.EBOOK, "로맨스", 56555),
                    Category.of(MallType.EBOOK, "만화", 38416),
                    Category.of(MallType.EBOOK, "사전/기타", 38419),
                    Category.of(MallType.EBOOK, "사회과학", 38404),
                    Category.of(MallType.EBOOK, "소설/시/희곡", 38396),
                    Category.of(MallType.EBOOK, "알라딘오디오북", 158585),
                    Category.of(MallType.EBOOK, "어린이", 38406),
                    Category.of(MallType.EBOOK, "에세이", 56387),
                    Category.of(MallType.EBOOK, "여행", 38408),
                    Category.of(MallType.EBOOK, "역사", 38397),
                    Category.of(MallType.EBOOK, "예술/대중문화", 38402),
                    Category.of(MallType.EBOOK, "외국어", 38411),
                    Category.of(MallType.EBOOK, "유아", 38424),
                    Category.of(MallType.EBOOK, "인문학", 38403),
                    Category.of(MallType.EBOOK, "자기계발", 38400),
                    Category.of(MallType.EBOOK, "장르소설", 112013),
                    Category.of(MallType.EBOOK, "전집/중고전집", 38426),
                    Category.of(MallType.EBOOK, "종교/역학", 38410),
                    Category.of(MallType.EBOOK, "좋은부모", 38413),
                    Category.of(MallType.EBOOK, "청소년", 38407),
                    Category.of(MallType.EBOOK, "컴퓨터/모바일", 38401),
                    Category.of(MallType.EBOOK, "판타지/무협", 78871),
                    Category.of(MallType.EBOOK, "해외도서", 38425),
                    Category.of(MallType.EBOOK, "BL", 139379)
            );
            categoryRepository.saveAll(categories);
            log.info("카테고리 초기 데이터가 저장되었습니다.");
        }
        else {
            log.info("카테고리 데이터가 이미 존재합니다. 초기화하지 않습니다.");
        }
    }
}
