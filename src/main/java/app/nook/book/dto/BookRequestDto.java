package app.nook.book.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class BookRequestDto {

    public record CreateUserBookRequest(
            @NotBlank(message = "제목은 필수입니다.")
            @Size(max = 1000, message = "제목은 1000자를 초과할 수 없습니다.")
            String title,

            @NotBlank(message = "저자는 필수입니다.")
            @Size(max = 1500, message = "저자는 1500자를 초과할 수 없습니다.")
            String author,

            @NotBlank(message = "카테고리는 필수입니다.")
            @Size(max = 50, message = "카테고리 이름이 너무 깁니다.")
            String categoryName,

            @Size(max = 500, message = "소개는 500자를 초과할 수 없습니다.")
            String description,

            @Min(value = 1, message = "페이지 수는 1 이상이어야 합니다.")
            Integer pages,

            @Size(max = 1500, message = "출판사는 1500자를 초과할 수 없습니다.")
            String publisher,

            String publicationDate,

            @Pattern(regexp = "^(|\\d{1,13})$", message = "ISBN은 숫자만 입력 가능하며 최대 13자리입니다.")
            String isbn13,

            MultipartFile coverImage
    ) {}

        public record UpdateUserBookRequest(
                @NotBlank(message = "제목은 필수입니다.")
                @Size(max = 1000, message = "제목은 1000자를 초과할 수 없습니다.")
                String title,

                @NotBlank(message = "저자는 필수입니다.")
                @Size(max = 1500, message = "저자는 1500자를 초과할 수 없습니다.")
                String author,

                @NotBlank(message = "카테고리는 필수입니다.")
                @Size(max = 50, message = "카테고리 이름이 너무 깁니다.")
                String categoryName,

                @Size(max = 500, message = "소개는 500자를 초과할 수 없습니다.")
                String description,

                @Min(value = 1, message = "페이지 수는 1 이상이어야 합니다.")
                Integer pages,

                @Size(max = 1500, message = "출판사는 1500자를 초과할 수 없습니다.")
                String publisher,

                String publicationDate,

                @Pattern(regexp = "^(|\\d{1,13})$", message = "ISBN은 숫자만 입력 가능하며 최대 13자리입니다.")
                String isbn13,

                MultipartFile coverImage
        ) {}

}
