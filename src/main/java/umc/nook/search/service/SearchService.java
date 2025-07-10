package umc.nook.search.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import umc.nook.aladin.dto.AladinResponseDTO;
import umc.nook.aladin.service.AladinService;
import umc.nook.lounge.converter.LoungeConverter;
import umc.nook.search.converter.SearchConverter;
import umc.nook.search.dto.SearchResponseDTO;
import umc.nook.users.domain.User;
import umc.nook.users.service.CustomUserDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private static int LIMIT = 10;

    private final AladinService aladinService;

    public Mono<SearchResponseDTO.SearchResultDTO> searchBooks(String query, int page, CustomUserDetails userDetails) {
        User user = userDetails.getUser();

        int start = (page - 1) * LIMIT + 1;

        return aladinService.searchBooks(query, start, LIMIT)
                .map(response -> {
                    List<SearchResponseDTO.BookDTO> books = new ArrayList<>();
                    if (response != null && response.getItem() != null) {
                        for (AladinResponseDTO.SearchBookDTO item : response.getItem()) {
                            books.add(SearchConverter.toBookDTO(item));
                        }
                    }
                    int totalItems = response != null ? response.getTotalResults() : 0;
                    int totalPages = totalItems > 0 ? (int) Math.ceil((double) totalItems / LIMIT) : 0;

                    return SearchResponseDTO.SearchResultDTO.builder()
                            .books(books)
                            .pagination(SearchConverter.toPaginiationDTO(page, LIMIT, totalItems, totalPages))
                            .build();
                });

    }

}
