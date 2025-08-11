package umc.nook.book.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.nook.aladin.dto.AladinResponseDTO;
import umc.nook.aladin.service.AladinService;
import umc.nook.book.converter.BookConverter;
import umc.nook.book.domain.Book;
import umc.nook.book.domain.Category;
import umc.nook.book.domain.MallType;
import umc.nook.book.dto.BookResponseDTO;
import umc.nook.book.repository.BookRepository;
import umc.nook.book.repository.CategoryRepository;
import umc.nook.book.utils.BookFilterUtils;
import umc.nook.bookshelves.repository.UserBookshelfRepository;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.review.service.ReviewService;
import umc.nook.users.domain.User;
import umc.nook.users.service.CustomUserDetails;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final AladinService aladinService;
    private final ReviewService reviewService;
    private final UserBookshelfRepository userBookshelfRepository;

    @Transactional
    public BookResponseDTO.BookDetailResultDTO getBookDetails(String isbn13, CustomUserDetails userDetails) {
        User user = userDetails.getUser();

        if (bookRepository.existsByIsbn13(isbn13)) {
            Book book = bookRepository.findByIsbn13(isbn13);
            boolean registeredBookshelf = userBookshelfRepository.existsByUserAndBook(user, book);
            BookResponseDTO.BookDetailDTO bookDetailDTO = BookConverter.toBookDetailDTO(book, registeredBookshelf   );
            List<BookResponseDTO.BestInThisCategoryDTO> bestList = getBestInThisCategory(
                    book.getCategory().getAladinCategoryId());

            return BookResponseDTO.BookDetailResultDTO.builder()
                    .book(bookDetailDTO)
                    .bestInThisCategory(bestList)
                    .build();
        }


        Book savedBook = addBook(isbn13);
//        boolean registeredBookshelf = userBookshelfRepository.existsByUserAndBook(user, savedBook);
        List<BookResponseDTO.BestInThisCategoryDTO> bestList =
                getBestInThisCategory(savedBook.getCategory().getAladinCategoryId());

        return BookResponseDTO.BookDetailResultDTO.builder()
                .book(BookConverter.toBookDetailDTO(savedBook, false))
                .bestInThisCategory(bestList)
                .build();
    }

    private List<BookResponseDTO.BestInThisCategoryDTO> getBestInThisCategory(int categoryId) {
        AladinResponseDTO.ResultDTO bestList = aladinService.fetchBooks("bestseller", null, 1, 5, String.valueOf(categoryId));
        List<BookResponseDTO.BestInThisCategoryDTO> bestInThisCategoryDTOList = new ArrayList<>();
        if (bestList != null && bestList.getItem() != null) {
            for (AladinResponseDTO.BookDetailDTO item : bestList.getItem()) {
                bestInThisCategoryDTOList.add(BookConverter.toBestInThisCategoryDTO(item));
            }
        }
        return bestInThisCategoryDTOList;
    }

    @Transactional
    public Book addBook(String isbn13) {
        AladinResponseDTO.ResultDTO result = aladinService.lookUpBook(isbn13);
        List<AladinResponseDTO.BookDetailDTO> items = result.getItem();

        if (items == null || items.isEmpty()) {
            throw new CustomException(ErrorCode.ISBN13_NOT_FOUND);
        }
        AladinResponseDTO.BookDetailDTO item = items.get(0);

        if (!BookFilterUtils.isBookIncluded(item.getCategoryName())) {
            throw new CustomException(ErrorCode.BOOK_NOT_ALLOWED);
        }
        return bookRepository.save(addBookByBookDetailDTO(item));
    }

    @Transactional
    public Book addBookByBookDetailDTO(AladinResponseDTO.BookDetailDTO bookDetailDTO) {
        Category category = getCategoryByFullName(bookDetailDTO.getCategoryName());
        Book bookEntity = BookConverter.toBook(bookDetailDTO, category);
        return bookRepository.save(bookEntity);
    }

    public Book findByIsbn13(String isbn13) {
        return bookRepository.findByIsbn13(isbn13);
    }

    public Category getCategoryByFullName(String categoryFullName) {
        String[] parts = categoryFullName.split(">");
        String mallType = parts[0].trim();
        String categoryName = parts[1].trim();
        return categoryRepository.findByCategoryNameAndMallType(categoryName, MallType.fromDisplayName(mallType))
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CATEGORY));
    }
}
