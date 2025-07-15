package umc.nook.search.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.search.converter.SearchConverter;
import umc.nook.search.domain.RecentQuery;
import umc.nook.search.dto.SearchResponseDTO;
import umc.nook.search.repository.RecentQueryRepository;
import umc.nook.users.domain.User;
import umc.nook.users.service.CustomUserDetails;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecentQueryService {
    private final RecentQueryRepository recentQueryRepository;

        @Transactional
        public void saveRecentQuery(User user, String query) {
            Optional<RecentQuery> existing = recentQueryRepository.findByUserAndQuery(user, query);

            if (existing.isPresent()) {
                RecentQuery recentQuery = existing.get();
                recentQuery.updateCreatedDatedNow();
            }
            else{
                long count = recentQueryRepository.countByUser(user);

                if (count >= 10) {
                    Optional<RecentQuery> oldest =
                            recentQueryRepository.findTopByUserOrderByCreatedDateAsc(user);
                    oldest.ifPresent(recentQueryRepository::delete);
                }

                RecentQuery newQuery = RecentQuery.builder()
                        .user(user)
                        .query(query)
                        .build();
                recentQueryRepository.save(newQuery);
            }
        }

    public SearchResponseDTO.RecentQueryResultDTO getRecentQueries(CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        List<RecentQuery> recentQueries = recentQueryRepository.findByUserOrderByCreatedDateDesc(user);

        return SearchConverter.toRecentQueryResultDTO(recentQueries.stream()
                .map(SearchConverter::toRecentQueryDTO)
                .toList());
    }

    @Transactional
    public void deleteRecentQuery(CustomUserDetails userDetails, Long recentQueryId) {

        User user = userDetails.getUser();
        Optional<RecentQuery> recentQueryOpt = recentQueryRepository.findById(recentQueryId);

        if (recentQueryOpt.isPresent() &&
            recentQueryOpt.get().getUser() != null &&
            recentQueryOpt.get().getUser().getUserId().equals(user.getUserId())) {

            recentQueryRepository.deleteById(recentQueryId);
        }
        else{
            throw new CustomException(ErrorCode.RECENT_QUERY_NOT_FOUND);
        }
    }

    @Transactional
    public void deleteAllRecentQueries(CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        recentQueryRepository.deleteAllByUser(user);
    }
}
