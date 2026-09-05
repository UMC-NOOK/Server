package app.nook.book.service;

import app.nook.book.domain.SearchHistory;
import app.nook.book.domain.enums.SearchType;
import app.nook.book.exception.SearchErrorCode;
import app.nook.book.repository.SearchHistoryRepository;
import app.nook.global.exception.CustomException;
import app.nook.global.response.AuthErrorCode;
import app.nook.user.domain.User;
import app.nook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 검색 기록 관리 서비스
 * - 검색어 저장, 조회, 삭제 기능을 제공
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;
    private static final int MAX_HISTORY_SIZE = 10;

    /**
     * 검색어 저장
     * - 중복된 키워드는 삭제 후 최신으로 갱신됨.
     * - 최대 10개까지만 저장되며, 초과 시 가장 오래된 기록이 삭제됨.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveKeyword(Long userId, String keyword, SearchType searchType) {
        if (keyword == null || keyword.isBlank()) {
            throw new CustomException(SearchErrorCode.INVALID_KEYWORD);
        }

        User user = findUserForUpdate(userId);

        // 중복 키워드 삭제
        searchHistoryRepository.findExisting(user, keyword, searchType)
                .ifPresent(history -> {
                    searchHistoryRepository.delete(history);
                    searchHistoryRepository.flush();
                });

        List<SearchHistory> histories = searchHistoryRepository
                .findAllRecent(user, searchType);

        // 최대 개수 초과 시 가장 오래된 기록 삭제
        if (histories.size() >= MAX_HISTORY_SIZE) {
            SearchHistory oldest = histories.get(histories.size() - 1);
            searchHistoryRepository.delete(oldest);
        }

        searchHistoryRepository.save(SearchHistory.builder()
                .user(user)
                .keyword(keyword)
                .searchType(searchType)
                .build());
    }


    /**
     * 특정 타입의 최근 검색어 목록 조회. (최신순)
     */
    public List<String> getSearchHistories(Long userId, SearchType searchType) {
        User user = findUser(userId);

        return searchHistoryRepository.findAllRecent(user, searchType).stream()
                .map(SearchHistory::getKeyword)
                .toList();
    }

    /**
     * 특정 검색어 삭제
     */
    @Transactional
    public void deleteHistory(Long userId, String keyword, SearchType searchType) {
        User user = findUserForUpdate(userId);

        searchHistoryRepository.findExisting(user, keyword, searchType)
                .ifPresent(searchHistoryRepository::delete);
    }

    /**
     * 해당 타입의 모든 검색 기록 삭제
     */
    @Transactional
    public void deleteAllHistories(Long userId, SearchType searchType) {
        User user = findUserForUpdate(userId);

        searchHistoryRepository.deleteAllByUserAndSearchType(user, searchType);
    }

    // 유저 조회 헬퍼 메서드
    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }

    private User findUserForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }
}
