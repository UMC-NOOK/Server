package app.nook.book.converter;

import app.nook.book.dto.BookResponseDto;
import app.nook.book.domain.Book;
import app.nook.book.domain.Category;
import app.nook.book.domain.enums.SourceType;
import app.nook.library.domain.Library;

public class BookConverter {
    public static BookResponseDto.BookDetailDto toBookDetailDto(Book book, Long bookShelfId) {
        Category category = book.getCategory();

        return BookResponseDto.BookDetailDto.builder()
                .bookId(book.getId())
                .isbn13(book.getIsbn13())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .publicationDate(book.getPublicationDate())
                .mallType(category.getMallType().getDisplayName())
                .mallTypeCode(category.getMallType())
                .category(category.getCategoryName())
                .pages(book.getPages())
                .description(book.getDescription())
                .coverImageUrl(book.getCoverImageUrl())
                .aladinLink(book.getAladinLink())
                .sourceType(book.getSourceType())
                .bookShelfId(bookShelfId)
                .build();
    }

    public static Book toBook(BookResponseDto.BookDetailDto bookDetailDto, Category category, SourceType sourceType) {
        return Book.builder()
                .isbn13(bookDetailDto.getIsbn13())
                .title(bookDetailDto.getTitle())
                .author(bookDetailDto.getAuthor())
                .publisher(bookDetailDto.getPublisher())
                .publicationDate(bookDetailDto.getPublicationDate())
                .pages(bookDetailDto.getPages())
                .description(bookDetailDto.getDescription())
                .coverImageUrl(bookDetailDto.getCoverImageUrl())
                .aladinLink(bookDetailDto.getAladinLink())
                .sourceType(sourceType)
                .category(category)
                .build();
    }


    // Library 엔티티 → BookSearchDto 변환 (서재 내 검색용)
    public static BookResponseDto.BookSearchDto toBookSearchDto(Library library) {
        Book book = library.getBook();
        Category category = book.getCategory();

        return BookResponseDto.BookSearchDto.builder()
                .isbn13(book.getIsbn13())
                .title(book.getTitle())
                .mallType(category.getMallType().getDisplayName())
                .author(book.getAuthor())
                .coverImageUrl(book.getCoverImageUrl())
                .publisher(book.getPublisher())
                .publicationDate(book.getPublicationDate())
                .isInLibrary(true)
                .readingStatus(library.getReadingStatus())
                .build();
    }
}
