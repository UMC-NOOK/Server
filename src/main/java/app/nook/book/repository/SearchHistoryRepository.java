package app.nook.book.repository;

import app.nook.book.domain.SearchHistory;
import app.nook.book.domain.enums.SearchType;
import app.nook.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    // 중복 키워드 확인
    @Query("SELECT sh FROM SearchHistory sh WHERE sh.user = :user AND sh.keyword = :keyword AND sh.searchType = :searchType")
    Optional<SearchHistory> findExisting(@Param("user") User user,
                                         @Param("keyword") String keyword,
                                         @Param("searchType") SearchType searchType);

    // 기록 조회 (최신순)
    @Query("SELECT sh FROM SearchHistory sh WHERE sh.user = :user AND sh.searchType = :searchType ORDER BY sh.modifiedDate DESC, sh.id DESC")
    List<SearchHistory> findAllRecent(@Param("user") User user, @Param("searchType") SearchType searchType);

    // 특정 타입의 기록 전체 삭제
    void deleteAllByUserAndSearchType(User user, SearchType searchType);

}
