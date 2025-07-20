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

    @Transactional
    public BookResponseDTO.BookDetailResultDTO getBookDetails(String isbn13, CustomUserDetails userDetails) {
        User user = userDetails.getUser();

        Book book = bookRepository.findByIsbn13(isbn13);
        if (book != null) {
            BookResponseDTO.BookDetailDTO bookDetailDTO = BookConverter.toBookDetailDTO(book);
            List<BookResponseDTO.BestInThisCategoryDTO> bestList = getBestInThisCategory(
                    book.getCategory().getAladinCategoryId());

            return BookResponseDTO.BookDetailResultDTO.builder()
                    .book(bookDetailDTO)
                    .reviewData(reviewService.getReviews(isbn13, userDetails, 1))
                    .bestInThisCategory(bestList)
                    .build();
        }

        AladinResponseDTO.LookUpResultDTO result = aladinService.lookUpBook(isbn13).block();
        List<AladinResponseDTO.BookDetailDTO> items = result.getItem();
        if (items == null || items.isEmpty()) {
            throw new CustomException(ErrorCode.ISBN13_NOT_FOUND);
        }
        AladinResponseDTO.BookDetailDTO item = items.get(0);

        if (!BookFilterUtils.isBookIncluded(item.getCategoryName())) {
            throw new CustomException(ErrorCode.BOOK_NOT_ALLOWED);
        }

        String[] parts = item.getCategoryName().split(">");
        String categoryName = parts[1].trim();
        Category category = categoryRepository.findByCategoryNameAndMallType(categoryName, MallType.valueOf(item.getMallType()))
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CATEGORY));

        Book bookEntity = Book.builder()
                .isbn13(item.getIsbn13())
                .title(item.getTitle())
                .author(item.getAuthor())
                .publisher(item.getPublisher())
                .publicationDate(item.getPubDate())
                .pages(item.getItemPage())
                .description(item.getDescription())
                .coverImageUrl(item.getCover())
                .category(category)
                .build();

        Book savedBook = bookRepository.save(bookEntity);

        List<BookResponseDTO.BestInThisCategoryDTO> bestList =
                getBestInThisCategory(category.getAladinCategoryId());

        return BookResponseDTO.BookDetailResultDTO.builder()
                .book(BookConverter.toBookDetailDTO(savedBook))
                .reviewData(reviewService.getReviews(isbn13, userDetails, 1))
                .bestInThisCategory(bestList)
                .build();
    }

    private List<BookResponseDTO.BestInThisCategoryDTO> getBestInThisCategory(int categoryId) {
        AladinResponseDTO.LoungeResultDTO bestList = aladinService.fetchBooks("bestseller", null, 1, 5, String.valueOf(categoryId))
                .block();
        List<BookResponseDTO.BestInThisCategoryDTO> bestInThisCategoryDTOList = new ArrayList<>();
        if (bestList != null && bestList.getItem() != null) {
            for (AladinResponseDTO.LoungeBookDTO item : bestList.getItem()) {
                bestInThisCategoryDTOList.add(BookConverter.toBestInThisCategoryDTO(item));
            }
        }
        return bestInThisCategoryDTOList;
    }
}
