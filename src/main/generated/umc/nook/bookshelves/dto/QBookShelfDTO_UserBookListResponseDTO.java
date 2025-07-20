package umc.nook.bookshelves.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * umc.nook.bookshelves.dto.QBookShelfDTO_UserBookListResponseDTO is a Querydsl Projection type for UserBookListResponseDTO
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QBookShelfDTO_UserBookListResponseDTO extends ConstructorExpression<BookShelfDTO.UserBookListResponseDTO> {

    private static final long serialVersionUID = 74969800L;

    public QBookShelfDTO_UserBookListResponseDTO(com.querydsl.core.types.Expression<Long> bookId, com.querydsl.core.types.Expression<String> title, com.querydsl.core.types.Expression<String> author, com.querydsl.core.types.Expression<String> publisher, com.querydsl.core.types.Expression<String> coverImageUrl, com.querydsl.core.types.Expression<String> readingStatus, com.querydsl.core.types.Expression<Integer> myRating) {
        super(BookShelfDTO.UserBookListResponseDTO.class, new Class<?>[]{long.class, String.class, String.class, String.class, String.class, String.class, int.class}, bookId, title, author, publisher, coverImageUrl, readingStatus, myRating);
    }

}

