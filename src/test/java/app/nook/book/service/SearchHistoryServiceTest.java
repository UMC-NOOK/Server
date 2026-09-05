package app.nook.book.service;

import app.nook.book.domain.SearchHistory;
import app.nook.book.domain.enums.SearchType;
import app.nook.book.exception.SearchErrorCode;
import app.nook.book.repository.SearchHistoryRepository;
import app.nook.global.exception.CustomException;
import app.nook.global.response.AuthErrorCode;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchHistoryServiceTest {

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_KEYWORD_1 = "채식주의자";
    private static final String TEST_KEYWORD_2 = "소년이 온다";
    private static final String TEST_EMAIL = "test@example.com";

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SearchHistoryService searchHistoryService;

    @Captor
    private ArgumentCaptor<SearchHistory> historyCaptor;

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
        ReflectionTestUtils.setField(testUser, "id", TEST_USER_ID);
    }

    @Test
    @DisplayName("검색어 저장 성공 - 새로운 검색어")
    void saveKeyword_새로운검색어_성공() {
        // given
        given(userRepository.findByIdForUpdate(TEST_USER_ID))
                .willReturn(Optional.of(testUser));
        given(searchHistoryRepository.findExisting(testUser, TEST_KEYWORD_1, SearchType.GLOBAL))
                .willReturn(Optional.empty()); // 중복 없음
        given(searchHistoryRepository.findAllRecent(testUser, SearchType.GLOBAL))
                .willReturn(new ArrayList<>()); // 기존 이력 없음

        // when
        searchHistoryService.saveKeyword(TEST_USER_ID, TEST_KEYWORD_1, SearchType.GLOBAL);

        // then
        InOrder inOrder = inOrder(userRepository, searchHistoryRepository);
        inOrder.verify(userRepository).findByIdForUpdate(TEST_USER_ID);
        inOrder.verify(searchHistoryRepository).findExisting(testUser, TEST_KEYWORD_1, SearchType.GLOBAL);
        inOrder.verify(searchHistoryRepository).findAllRecent(testUser, SearchType.GLOBAL);
        inOrder.verify(searchHistoryRepository).save(historyCaptor.capture());
        SearchHistory saved = historyCaptor.getValue();

        assertThat(saved.getUser()).isEqualTo(testUser);
        assertThat(saved.getKeyword()).isEqualTo(TEST_KEYWORD_1);
        assertThat(saved.getSearchType()).isEqualTo(SearchType.GLOBAL);
    }

    @Test
    @DisplayName("검색어 저장 - 중복 검색어 삭제 후 새로 저장")
    void saveKeyword_중복검색어_삭제후저장() {
        // given
        SearchHistory existingHistory = createSearchHistory(TEST_KEYWORD_1, SearchType.GLOBAL);

        given(userRepository.findByIdForUpdate(TEST_USER_ID))
                .willReturn(Optional.of(testUser));
        given(searchHistoryRepository.findExisting(testUser, TEST_KEYWORD_1, SearchType.GLOBAL))
                .willReturn(Optional.of(existingHistory)); // 중복 존재
        given(searchHistoryRepository.findAllRecent(testUser, SearchType.GLOBAL))
                .willReturn(new ArrayList<>());

        // when
        searchHistoryService.saveKeyword(TEST_USER_ID, TEST_KEYWORD_1, SearchType.GLOBAL);

        // then
        InOrder inOrder = inOrder(userRepository, searchHistoryRepository);
        inOrder.verify(userRepository).findByIdForUpdate(TEST_USER_ID);
        inOrder.verify(searchHistoryRepository).findExisting(testUser, TEST_KEYWORD_1, SearchType.GLOBAL);
        inOrder.verify(searchHistoryRepository).delete(existingHistory);
        inOrder.verify(searchHistoryRepository).flush();
        inOrder.verify(searchHistoryRepository).findAllRecent(testUser, SearchType.GLOBAL);
        inOrder.verify(searchHistoryRepository).save(historyCaptor.capture());
        SearchHistory saved = historyCaptor.getValue();

        // 데이터 정합성 검증
        assertThat(saved.getKeyword()).isEqualTo(TEST_KEYWORD_1);
        assertThat(saved.getUser()).isEqualTo(testUser);
        assertThat(saved.getSearchType()).isEqualTo(SearchType.GLOBAL);
    }

    @Test
    @DisplayName("검색어 저장 - 10개 초과 시 가장 오래된 기록 삭제")
    void saveKeyword_10개초과_가장오래된기록삭제() {
        // given
        List<SearchHistory> histories = createMultipleHistories(10); // 10개 꽉 찬 상태
        SearchHistory oldest = histories.get(9); // 리스트의 마지막이 가장 오래된 것

        given(userRepository.findByIdForUpdate(TEST_USER_ID))
                .willReturn(Optional.of(testUser));
        given(searchHistoryRepository.findExisting(testUser, "새로운검색어", SearchType.GLOBAL))
                .willReturn(Optional.empty());
        given(searchHistoryRepository.findAllRecent(testUser, SearchType.GLOBAL))
                .willReturn(histories);

        // when
        searchHistoryService.saveKeyword(TEST_USER_ID, "새로운검색어", SearchType.GLOBAL);

        // then
        InOrder inOrder = inOrder(userRepository, searchHistoryRepository);
        inOrder.verify(userRepository).findByIdForUpdate(TEST_USER_ID);
        inOrder.verify(searchHistoryRepository).findExisting(testUser, "새로운검색어", SearchType.GLOBAL);
        inOrder.verify(searchHistoryRepository).findAllRecent(testUser, SearchType.GLOBAL);
        inOrder.verify(searchHistoryRepository).delete(oldest);
        inOrder.verify(searchHistoryRepository).save(any(SearchHistory.class));
    }

    @Test
    @DisplayName("검색어 저장 실패 - 빈 검색어")
    void saveKeyword_빈검색어_예외발생() {
        // when & then
        CustomException ex = assertThrows(
                CustomException.class,
                () -> searchHistoryService.saveKeyword(TEST_USER_ID, "", SearchType.GLOBAL)
        );

        assertThat(ex.getErrorCode()).isEqualTo(SearchErrorCode.INVALID_KEYWORD);
        verify(searchHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("검색어 저장 실패 - null 검색어")
    void saveKeyword_null검색어_예외발생() {
        // when & then
        CustomException ex = assertThrows(
                CustomException.class,
                () -> searchHistoryService.saveKeyword(TEST_USER_ID, null, SearchType.GLOBAL)
        );

        assertThat(ex.getErrorCode()).isEqualTo(SearchErrorCode.INVALID_KEYWORD);
    }

    @Test
    @DisplayName("검색어 저장 실패 - 존재하지 않는 유저")
    void saveKeyword_유저없음_예외발생() {
        // given
        given(userRepository.findByIdForUpdate(TEST_USER_ID))
                .willReturn(Optional.empty());

        // when & then
        CustomException ex = assertThrows(
                CustomException.class,
                () -> searchHistoryService.saveKeyword(TEST_USER_ID, TEST_KEYWORD_1, SearchType.GLOBAL)
        );

        assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("검색 이력 조회 성공")
    void getSearchHistories_성공() {
        // given
        List<SearchHistory> histories = Arrays.asList(
                createSearchHistory(TEST_KEYWORD_1, SearchType.GLOBAL),
                createSearchHistory(TEST_KEYWORD_2, SearchType.GLOBAL)
        );

        given(userRepository.findById(TEST_USER_ID))
                .willReturn(Optional.of(testUser));
        given(searchHistoryRepository.findAllRecent(testUser, SearchType.GLOBAL))
                .willReturn(histories);

        // when
        List<String> result = searchHistoryService.getSearchHistories(TEST_USER_ID, SearchType.GLOBAL);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(TEST_KEYWORD_1);
        assertThat(result.get(1)).isEqualTo(TEST_KEYWORD_2);

        verify(searchHistoryRepository, times(1)).findAllRecent(testUser, SearchType.GLOBAL);
    }

    @Test
    @DisplayName("특정 검색어 삭제 성공")
    void deleteHistory_성공() {
        // given
        SearchHistory history = createSearchHistory(TEST_KEYWORD_1, SearchType.GLOBAL);

        given(userRepository.findByIdForUpdate(TEST_USER_ID))
                .willReturn(Optional.of(testUser));
        given(searchHistoryRepository.findExisting(testUser, TEST_KEYWORD_1, SearchType.GLOBAL))
                .willReturn(Optional.of(history));

        // when
        searchHistoryService.deleteHistory(TEST_USER_ID, TEST_KEYWORD_1, SearchType.GLOBAL);

        // then
        InOrder inOrder = inOrder(userRepository, searchHistoryRepository);
        inOrder.verify(userRepository).findByIdForUpdate(TEST_USER_ID);
        inOrder.verify(searchHistoryRepository).findExisting(testUser, TEST_KEYWORD_1, SearchType.GLOBAL);
        inOrder.verify(searchHistoryRepository).delete(history);
    }

    @Test
    @DisplayName("특정 검색어 삭제 - 존재하지 않는 검색어는 무시")
    void deleteHistory_존재하지않는검색어_무시() {
        // given
        given(userRepository.findByIdForUpdate(TEST_USER_ID))
                .willReturn(Optional.of(testUser));
        given(searchHistoryRepository.findExisting(testUser, "없는검색어", SearchType.GLOBAL))
                .willReturn(Optional.empty());

        // when
        searchHistoryService.deleteHistory(TEST_USER_ID, "없는검색어", SearchType.GLOBAL);

        // then
        InOrder inOrder = inOrder(userRepository, searchHistoryRepository);
        inOrder.verify(userRepository).findByIdForUpdate(TEST_USER_ID);
        inOrder.verify(searchHistoryRepository).findExisting(testUser, "없는검색어", SearchType.GLOBAL);
        verify(searchHistoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("전체 검색 이력 삭제 성공")
    void deleteAllHistories_성공() {
        // given
        given(userRepository.findByIdForUpdate(TEST_USER_ID))
                .willReturn(Optional.of(testUser));

        // when
        searchHistoryService.deleteAllHistories(TEST_USER_ID, SearchType.GLOBAL);

        // then
        InOrder inOrder = inOrder(userRepository, searchHistoryRepository);
        inOrder.verify(userRepository).findByIdForUpdate(TEST_USER_ID);
        inOrder.verify(searchHistoryRepository).deleteAllByUserAndSearchType(testUser, SearchType.GLOBAL);
    }

    private SearchHistory createSearchHistory(String keyword, SearchType searchType) {
        return SearchHistory.builder()
                .user(testUser)
                .keyword(keyword)
                .searchType(searchType)
                .build();
    }

    private List<SearchHistory> createMultipleHistories(int count) {
        List<SearchHistory> histories = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            histories.add(createSearchHistory("검색어" + i, SearchType.GLOBAL));
        }
        return histories;
    }
}
