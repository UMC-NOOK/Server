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
            for (AladinResponseDTO.BookDetailDTO item : response.getItem()) {
                if (isValidBook(item)) {
                    System.out.println("item title = " + item.getTitle());
                    System.out.println("item category = " + item.getCategoryName());
                    Book book = bookService.findByIsbn13(item.getIsbn13());
                    if (book == null) {
                        book = bookService.addBook(item.getIsbn13());
                    }
                    // 책의 필수 정보가 비어있는 경우
                    if (book == null) {
                        continue;
                    }
                    books.add(SearchConverter.toBookDTO(item, book.getBookId()));
                }
                if (books.size() == LIMIT) {
                    break;
                }
            }
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

    private boolean isValidBook(AladinResponseDTO.BookDetailDTO item) {
        if (item.getIsbn13() == null || item.getIsbn13().isBlank() || item.getIsbn13().length() != 13) return false;
        if (item.getCategoryName() == null || item.getCategoryName().isBlank()) return false;
        if (!BookFilterUtils.isValidMallType(item.getMallType())) return false;

        // depth(>) 최소 2개 이상 체크
        String[] parts = item.getCategoryName().split(">");
        if (parts.length < 2) return false;

        return BookFilterUtils.isBookIncluded(item.getCategoryName());
    }

}
