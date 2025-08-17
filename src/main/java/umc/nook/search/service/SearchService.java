package umc.nook.search.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.nook.aladin.dto.AladinResponseDTO;
import umc.nook.aladin.service.AladinService;
import umc.nook.book.domain.Book;
import umc.nook.book.service.BookService;
import umc.nook.book.utils.BookFilterUtils;
import umc.nook.bookshelves.repository.UserBookshelfRepository;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.search.converter.SearchConverter;
import umc.nook.search.dto.SearchResponseDTO;
import umc.nook.users.domain.User;
import umc.nook.users.service.CustomUserDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private static final int LIMIT = 10;

    private final AladinService aladinService;
    private final BookService bookService;
    private final RecentQueryService recentQueryService;

    @Transactional
    public SearchResponseDTO.SearchResultDTO searchBooks(String query, int page, CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        int fetchSize = LIMIT * 2;

        AladinResponseDTO.ResultDTO response = aladinService.searchBooks(query, page+1, fetchSize);
        List<SearchResponseDTO.BookDTO> books = new ArrayList<>();
        if (response != null && response.getItem() != null) {
            List<AladinResponseDTO.BookDetailDTO> validItems = response.getItem().stream()
                    .filter(BookFilterUtils::isValidBook)
                    .toList();
            List<String> isbn13List = validItems.stream()
                    .map(AladinResponseDTO.BookDetailDTO::getIsbn13)
                    .distinct()
                    .toList();
            Map<String, Book> bookMap = bookService.findBookByIsbn13List(isbn13List);

            List<String> missingIsbn13List = isbn13List.stream()
                    .filter(isbn13 -> !bookMap.containsKey(isbn13))
                    .toList();
            if (!missingIsbn13List.isEmpty()) {
                List<Book> newBooks = bookService.addBooksBatch(missingIsbn13List);
                newBooks.forEach(book -> bookMap.put(book.getIsbn13(), book));
            }
            books = validItems.stream()
                    .map(item -> {
                        Book book = bookMap.get(item.getIsbn13());
                        return (book != null) ? SearchConverter.toBookDTO(item, book.getBookId()) : null;
                    })
                    .filter(Objects::nonNull)
                    .limit(LIMIT)
                    .toList();
        }

        int totalItems = response != null ? response.getTotalResults() : 0;
        int totalPages = totalItems > 0 ? (int) Math.ceil((double) totalItems / fetchSize): 0;
        if ((totalPages == 0 && page > 0) || (totalPages > 0 && page >= totalPages)) {
            throw new CustomException(ErrorCode.PAGE_OUT_OF_RANGE);
        }
        recentQueryService.saveRecentQuery(user, query);
        return SearchResponseDTO.SearchResultDTO.builder()
                .books(books)
                .pagination(SearchConverter.toPaginationDTO(page, LIMIT, totalItems, totalPages))
                .build();

    }
}
