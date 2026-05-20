package app.nook.focus.service;

import app.nook.focus.domain.Focus;
import app.nook.focus.domain.Theme;
import app.nook.focus.dto.FocusResponseDto;
import app.nook.focus.repository.FocusRepository;
import app.nook.global.dto.CursorResponse;
import app.nook.global.fixture.FocusFixture;
import app.nook.global.fixture.LibraryFixture;
import app.nook.global.fixture.ThemeFixture;
import app.nook.global.fixture.UserFixture;
import app.nook.library.domain.Library;
import app.nook.r2.service.PresignedUrlService;
import app.nook.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("FocusQueryService 테스트")
class FocusQueryServiceTest {

    @Mock
    private FocusRepository focusRepository;

    @Mock
    private PresignedUrlService presignedUrlService;

    @InjectMocks
    private FocusQueryService focusQueryService;

    private User user;
    private Library library;
    private Theme theme;
    private Focus completedFocus;

    @BeforeEach
    void setUp() {
        user = UserFixture.user();
        library = LibraryFixture.library(user, FocusFixture.book());
        theme = ThemeFixture.theme();
        completedFocus = FocusFixture.completedFocus(library, theme);
    }

    @Nested
    @DisplayName("최근 포커스 조회")
    class GetRecentFocuses {

        @Test
        @DisplayName("성공 - 다음 페이지 없음")
        void 성공_다음페이지없음() {
            Slice<Focus> slice = new SliceImpl<>(List.of(completedFocus), PageRequest.of(0, 10), false);

            given(focusRepository.findRecentByUserWithCursor(any(User.class), any(), any()))
                    .willReturn(slice);
            given(presignedUrlService.resolveImageUrl(any(), any()))
                    .willReturn("https://cdn.nook.com/covers/book.jpg");

            CursorResponse<FocusResponseDto.RecentFocusItem, Long> result =
                    focusQueryService.getRecentFocuses(user, null, 10);

            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.isHasNext()).isFalse();
            assertThat(result.getNextCursor()).isNull();

            FocusResponseDto.RecentFocusItem item = result.getItems().get(0);
            assertThat(item.focusId()).isEqualTo(completedFocus.getId());
            assertThat(item.bookId()).isEqualTo(library.getBook().getId());
            assertThat(item.coverImageUrl()).isEqualTo("https://cdn.nook.com/covers/book.jpg");
            assertThat(item.durationText()).isEqualTo("00:34:26");
        }

        @Test
        @DisplayName("성공 - 다음 페이지 있음")
        void 성공_다음페이지있음() {
            Slice<Focus> slice = new SliceImpl<>(List.of(completedFocus), PageRequest.of(0, 1), true);

            given(focusRepository.findRecentByUserWithCursor(any(User.class), any(), any()))
                    .willReturn(slice);
            given(presignedUrlService.resolveImageUrl(any(), any()))
                    .willReturn("https://cdn.nook.com/covers/book.jpg");

            CursorResponse<FocusResponseDto.RecentFocusItem, Long> result =
                    focusQueryService.getRecentFocuses(user, null, 1);

            assertThat(result.isHasNext()).isTrue();
            assertThat(result.getNextCursor()).isEqualTo(completedFocus.getId());
        }

        @Test
        @DisplayName("성공 - 결과 없음")
        void 성공_결과없음() {
            Slice<Focus> slice = new SliceImpl<>(List.of(), PageRequest.of(0, 10), false);

            given(focusRepository.findRecentByUserWithCursor(any(User.class), any(), any()))
                    .willReturn(slice);

            CursorResponse<FocusResponseDto.RecentFocusItem, Long> result =
                    focusQueryService.getRecentFocuses(user, null, 10);

            assertThat(result.getItems()).isEmpty();
            assertThat(result.isHasNext()).isFalse();
            assertThat(result.getNextCursor()).isNull();
        }
    }
}
