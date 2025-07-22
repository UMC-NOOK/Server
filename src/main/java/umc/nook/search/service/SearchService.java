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
import umc.nook.search.converter.SearchConverter;
import umc.nook.search.dto.SearchResponseDTO;
import umc.nook.users.domain.User;
import umc.nook.users.service.CustomUserDetails;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private static int LIMIT = 10;

    private final AladinService aladinService;
    private final BookService bookService;
    private final RecentQueryService recentQueryService;
    private final UserBookshelfRepository userBookshelfRepository;

    @Transactional
    public SearchResponseDTO.SearchResultDTO searchBooks(String query, int page, CustomUserDetails userDetails) {
        User user = userDetails.getUser();

        int start = (page - 1) * LIMIT + 1;

        AladinResponseDTO.ResultDTO response = aladinService.searchBooks(query, start, LIMIT);
        List<SearchResponseDTO.BookDTO> books = new ArrayList<>();
        if (response != null && response.getItem() != null) {
            for (AladinResponseDTO.BookDetailDTO item : response.getItem()) {
                if (BookFilterUtils.isBookIncluded(item.getCategoryName())) {
                    Book book = bookService.findByIsbn13(item.getIsbn13());
                    if (book == null) {
                        book = bookService.addBook(item.getIsbn13());
                    }
                    boolean registeredBookshelf = userBookshelfRepository.existsByUserAndBook(user, book);
                    books.add(SearchConverter.toBookDTO(item, book.getBookId(), registeredBookshelf));
                }
            }
        }
        int totalItems = response != null ? response.getTotalResults() : 0;
        int totalPages = totalItems > 0 ? (int) Math.ceil((double) totalItems / LIMIT) : 0;
        recentQueryService.saveRecentQuery(user, query);
        return SearchResponseDTO.SearchResultDTO.builder()
                .books(books)
                .pagination(SearchConverter.toPaginationDTO(page, LIMIT, totalItems, totalPages))
                .build();

    }
}
