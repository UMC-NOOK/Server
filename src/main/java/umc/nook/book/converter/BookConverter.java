package umc.nook.book.converter;

import umc.nook.aladin.dto.AladinResponseDTO;
import umc.nook.book.domain.Book;
import umc.nook.book.dto.BookResponseDTO;

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

    public static BookResponseDTO.BestInThisCategoryDTO toBestInThisCategoryDTO(AladinResponseDTO.LoungeBookDTO bookDTO) {
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
}
