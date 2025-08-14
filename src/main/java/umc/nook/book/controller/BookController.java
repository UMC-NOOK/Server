package umc.nook.book.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import umc.nook.book.dto.BookResponseDTO;
import umc.nook.book.service.BookService;
import umc.nook.book.validaton.annotation.ValidatedIsbn;
import umc.nook.common.response.ApiResponse;
import umc.nook.common.response.SuccessCode;
import umc.nook.users.service.CustomUserDetails;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/books")
@Validated
@Tag(name = "book", description = "책 API")
public class BookController {

    private final BookService bookService;

    @Operation(
            summary = "책 상세 정보 조회",
            description = """
                    ISBN-13을 사용하여 특정 도서의 상세 정보를 조회합니다.
                    리뷰 목록과 해당 분야의 베스트셀러 목록도 함께 제공합니다.
                    """
    )
    @Parameters({
            @Parameter(
                    name = "isbn13",
                    description = "조회할 도서의 isbn13",
                    required = true,
                    example = "9791161571188"
            )
    })
    @GetMapping("/{isbn13}")
    public ApiResponse<BookResponseDTO.BookDetailResultDTO> getBookDetails(
            @ValidatedIsbn @PathVariable String isbn13,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.onSuccess(bookService.getBookDetails(isbn13, userDetails), SuccessCode.OK);
    }
}
