package app.nook.book.repository;

import app.nook.book.domain.SearchHistory;
import app.nook.book.domain.enums.SearchType;
import app.nook.global.config.QueryDslConfig;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(QueryDslConfig.class)
class SearchHistoryRepositoryTest {

    private static final String TEST_KEYWORD_1 = "채식주의자";
    private static final String TEST_KEYWORD_2 = "소년이 온다";
    private static final String TEST_EMAIL = "test@example.com";

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email(TEST_EMAIL)
                .nickName("테스터")
                .provider("google")
                .providerId("provider-1")
                .role(UserRole.USER)
                .build();
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("검색 이력 저장 성공")
    void 검색이력_저장_성공() {
        // given
        SearchHistory history = createSearchHistory(TEST_KEYWORD_1, SearchType.GLOBAL);

        // when
        SearchHistory saved = searchHistoryRepository.save(history);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getKeyword()).isEqualTo(TEST_KEYWORD_1);
        assertThat(saved.getSearchType()).isEqualTo(SearchType.GLOBAL);
        assertThat(saved.getUser()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("중복 검색어 확인 - 존재하는 경우")
    void findExisting_존재() {
        // given
        SearchHistory history = createSearchHistory(TEST_KEYWORD_1, SearchType.GLOBAL);
        searchHistoryRepository.save(history);

        // when
        Optional<SearchHistory> found = searchHistoryRepository.findExisting(
                testUser, TEST_KEYWORD_1, SearchType.GLOBAL);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getKeyword()).isEqualTo(TEST_KEYWORD_1);
    }

    @Test
    @DisplayName("중복 검색어 확인 - 존재하지 않는 경우")
    void findExisting_존재하지않음() {
        // when
        Optional<SearchHistory> found = searchHistoryRepository.findExisting(
                testUser, "존재하지않는검색어", SearchType.GLOBAL);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("최신순으로 검색 이력 조회")
    void findAllRecent_최신순_조회() {
        // given
        SearchHistory history1 = createSearchHistory(TEST_KEYWORD_1, SearchType.GLOBAL);
        SearchHistory history2 = createSearchHistory(TEST_KEYWORD_2, SearchType.GLOBAL);

        SearchHistory saved1 = searchHistoryRepository.save(history1);
        SearchHistory saved2 = searchHistoryRepository.save(history2);

        // when
        List<SearchHistory> histories = searchHistoryRepository.findAllRecent(testUser, SearchType.GLOBAL);

        // then
        assertThat(histories).hasSize(2);
        // ID가 큰 것이 먼저 (최신이 먼저)
        assertThat(histories.get(0).getId()).isGreaterThan(histories.get(1).getId());
        assertThat(histories.get(0).getKeyword()).isEqualTo(TEST_KEYWORD_2);
        assertThat(histories.get(1).getKeyword()).isEqualTo(TEST_KEYWORD_1);
    }

    @Test
    @DisplayName("검색 타입별 이력 조회 - GLOBAL만 조회")
    void findAllRecent_타입별_조회() {
        // given
        searchHistoryRepository.save(createSearchHistory(TEST_KEYWORD_1, SearchType.GLOBAL));
        searchHistoryRepository.save(createSearchHistory(TEST_KEYWORD_2, SearchType.LIBRARY));

        // when
        List<SearchHistory> globalHistories = searchHistoryRepository.findAllRecent(testUser, SearchType.GLOBAL);
        List<SearchHistory> bookcaseHistories = searchHistoryRepository.findAllRecent(testUser, SearchType.LIBRARY);

        // then
        assertThat(globalHistories).hasSize(1);
        assertThat(globalHistories.get(0).getKeyword()).isEqualTo(TEST_KEYWORD_1);

        assertThat(bookcaseHistories).hasSize(1);
        assertThat(bookcaseHistories.get(0).getKeyword()).isEqualTo(TEST_KEYWORD_2);
    }

    @Test
    @DisplayName("특정 타입의 검색 이력 전체 삭제")
    void deleteAllByUserAndSearchType_성공() {
        // given
        searchHistoryRepository.save(createSearchHistory(TEST_KEYWORD_1, SearchType.GLOBAL));
        searchHistoryRepository.save(createSearchHistory(TEST_KEYWORD_2, SearchType.GLOBAL));
        searchHistoryRepository.save(createSearchHistory("다른 검색어", SearchType.LIBRARY));

        // when
        searchHistoryRepository.deleteAllByUserAndSearchType(testUser, SearchType.GLOBAL);

        // then
        List<SearchHistory> globalHistories = searchHistoryRepository.findAllRecent(testUser, SearchType.GLOBAL);
        List<SearchHistory> bookcaseHistories = searchHistoryRepository.findAllRecent(testUser, SearchType.LIBRARY);

        assertThat(globalHistories).isEmpty();
        assertThat(bookcaseHistories).hasSize(1); // LIBRARY는 삭제 안 됨
    }

    @Test
    @DisplayName("다른 유저의 검색 이력은 조회되지 않음")
    void 다른유저_검색이력_분리() {
        // given
        User anotherUser = User.builder()
                .email("another@example.com")
                .nickName("다른유저")
                .provider("google")
                .providerId("provider-2")
                .role(UserRole.USER)
                .build();
        userRepository.save(anotherUser);

        searchHistoryRepository.save(createSearchHistory(TEST_KEYWORD_1, SearchType.GLOBAL));

        SearchHistory anotherUserHistory = SearchHistory.builder()
                .user(anotherUser)
                .keyword(TEST_KEYWORD_2)
                .searchType(SearchType.GLOBAL)
                .build();
        searchHistoryRepository.save(anotherUserHistory);

        // when
        List<SearchHistory> testUserHistories = searchHistoryRepository.findAllRecent(testUser, SearchType.GLOBAL);
        List<SearchHistory> anotherUserHistories = searchHistoryRepository.findAllRecent(anotherUser, SearchType.GLOBAL);

        // then
        assertThat(testUserHistories).hasSize(1);
        assertThat(testUserHistories.get(0).getKeyword()).isEqualTo(TEST_KEYWORD_1);

        assertThat(anotherUserHistories).hasSize(1);
        assertThat(anotherUserHistories.get(0).getKeyword()).isEqualTo(TEST_KEYWORD_2);
    }

    private SearchHistory createSearchHistory(String keyword, SearchType searchType) {
        return SearchHistory.builder()
                .user(testUser)
                .keyword(keyword)
                .searchType(searchType)
                .build();
    }
}
