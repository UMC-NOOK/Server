package umc.nook.book.converter;

import umc.nook.aladin.dto.AladinResponseDTO;
import umc.nook.book.domain.Book;
import umc.nook.book.domain.Category;
import umc.nook.book.domain.MallType;
import umc.nook.book.dto.BookResponseDTO;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;

public class BookConverter {

    public static BookResponseDTO.BookDetailDTO toBookDetailDTO(AladinResponseDTO.BookDetailDTO aladinDTO) {
        String[] categoryParts = aladinDTO.getCategoryName().split(">");
        String categoryName = categoryParts[1].trim();
        return BookResponseDTO.BookDetailDTO.builder()
                .isbn13(aladinDTO.getIsbn13())
                .title(aladinDTO.getTitle())
                .author(aladinDTO.getAuthor())
                .publisher(aladinDTO.getPublisher())
                .pages(aladinDTO.getItemPage())
                .publicationDate(aladinDTO.getPubDate())
                .mallType(aladinDTO.getMallType())
                .category(categoryName)
                .description(aladinDTO.getDescription())
                .coverImageUrl(aladinDTO.getCover())
                .build();
    }

    public static BookResponseDTO.BestInThisCategoryDTO toBestInThisCategoryDTO(AladinResponseDTO.BookDetailDTO bookDTO) {
        return BookResponseDTO.BestInThisCategoryDTO.builder()
                .isbn13(bookDTO.getIsbn13())
                .title(bookDTO.getTitle())
                .author(bookDTO.getAuthor())
                .publisher(bookDTO.getPublisher())
                .coverImageUrl(bookDTO.getCover())
                .build();
    }

    public static BookResponseDTO.BookDetailDTO toBookDetailDTO(Book book) {
        return BookResponseDTO.BookDetailDTO.builder()
                .bookId(book.getBookId())
                .isbn13(book.getIsbn13())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publisher(book.getPublisher())
                .pages(book.getPages())
                .publicationDate(book.getPublicationDate())
                .mallType(String.valueOf(book.getCategory().getMallType()))
                .category(book.getCategory().getCategoryName())
                .description(book.getDescription())
                .coverImageUrl(book.getCoverImageUrl())
                .build();
    }

    public static Book toBook(AladinResponseDTO.BookDetailDTO bookDetailDTO, Category category) {
        return Book.builder()
                .isbn13(bookDetailDTO.getIsbn13())
                .title(bookDetailDTO.getTitle())
                .author(bookDetailDTO.getAuthor())
                .publisher(bookDetailDTO.getPublisher())
                .publicationDate(bookDetailDTO.getPubDate())
                .pages(bookDetailDTO.getItemPage())
                .description(bookDetailDTO.getDescription())
                .coverImageUrl(bookDetailDTO.getCover())
                .category(category)
                .build();
    }
}
