package umc.nook.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import umc.nook.search.domain.RecentQuery;
import umc.nook.users.domain.User;

import java.util.List;
import java.util.Optional;

public interface RecentQueryRepository extends JpaRepository<RecentQuery, Long> {
    Optional<RecentQuery> findByUserAndQuery(User user, String query);
    long countByUser(User user);
    Optional<RecentQuery> findTopByUserOrderByCreatedDateAsc(User user);
    List<RecentQuery> findByUserOrderByCreatedDateDesc(User user);

    void deleteAllByUser(User user);
}
