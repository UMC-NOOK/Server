package app.nook.book.service;

import app.nook.aladin.service.AladinService;
import app.nook.book.dto.BookResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PersonalizedBestsellerCacheServiceTest {

    private static final String TEST_AUTHOR = "한강";
    private static final String TEST_PUBLISHER = "창비";

    @Mock
    private AladinService aladinService;

    @InjectMocks
    private PersonalizedBestsellerCacheService personalizedBestsellerCacheService;

    @Test
    @DisplayName("카테고리 ID로 맞춤형 베스트셀러를 조회한다")
    void getByCategoryId_success() {
        List<BookResponseDto.BookPreviewDto> expected = List.of(
                createBookPreviewDto("9788936434267", "채식주의자", 1)
        );

        given(aladinService.fetchItemList("Bestseller", "BOOK", 5, "42"))
                .willReturn(expected);

        List<BookResponseDto.BookPreviewDto> result =
                personalizedBestsellerCacheService.getByCategoryId(42);

        assertThat(result).isEqualTo(expected);
        verify(aladinService, times(1)).fetchItemList("Bestseller", "BOOK", 5, "42");
    }

    private BookResponseDto.BookPreviewDto createBookPreviewDto(String isbn13, String title, Integer rank) {
        return new BookResponseDto.BookPreviewDto(
                isbn13,
                title,
                TEST_AUTHOR,
                "http://example.com/cover.jpg",
                TEST_PUBLISHER,
                rank
        );
    }
}
