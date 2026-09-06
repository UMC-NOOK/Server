package app.nook.focus.service;

import app.nook.focus.domain.Focus;
import app.nook.focus.dto.FocusResponseDto;
import app.nook.focus.repository.FocusRepository;
import app.nook.focus.repository.dto.MonthlyFocusStatsDto;
import app.nook.global.dto.CursorResponse;
import app.nook.global.fixture.FocusFixture;
import app.nook.global.fixture.LibraryFixture;
import app.nook.global.fixture.UserFixture;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.dto.LibraryViewDto;
import app.nook.library.service.LibraryQueryService;
import app.nook.r2.service.PresignedUrlService;
import app.nook.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("FocusQueryService 테스트")
class FocusQueryServiceTest {

    @Mock
    private FocusRepository focusRepository;

    @Mock
    private PresignedUrlService presignedUrlService;

    @Mock
    private LibraryQueryService libraryQueryService;

    private FocusQueryService focusQueryService;

    private User user;
    private Library library;
    private Focus completedFocus;

    @BeforeEach
    void setUp() {
        user = UserFixture.user();
        library = LibraryFixture.library(user, FocusFixture.book());
        completedFocus = FocusFixture.completedFocus(library);
        Clock clock = Clock.fixed(Instant.parse("2026-03-22T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        focusQueryService = new FocusQueryService(
                focusRepository,
                presignedUrlService,
                libraryQueryService,
                clock
        );
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
            assertThat(result.items()).hasSize(1);
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();

            FocusResponseDto.RecentFocusItem item = result.items().get(0);
            assertThat(item.focusId()).isEqualTo(completedFocus.getId());
            assertThat(item.bookId()).isEqualTo(library.getBook().getId());
            assertThat(item.coverImageUrl()).isEqualTo("https://cdn.nook.com/covers/book.jpg");
            assertThat(item.durationText()).isEqualTo("00:34:26");
            verify(focusRepository).findRecentByUserWithCursor(user, null, PageRequest.of(0, 10));
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

            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo(completedFocus.getId());
        }

        @Test
        @DisplayName("성공 - 결과 없음")
        void 성공_결과없음() {
            Slice<Focus> slice = new SliceImpl<>(List.of(), PageRequest.of(0, 10), false);

            given(focusRepository.findRecentByUserWithCursor(any(User.class), any(), any()))
                    .willReturn(slice);

            CursorResponse<FocusResponseDto.RecentFocusItem, Long> result =
                    focusQueryService.getRecentFocuses(user, null, 10);

            assertThat(result.items()).isEmpty();
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
        }
    }

    @Nested
    @DisplayName("포커스 홈 조회")
    class GetFocusHome {

        @Test
        @DisplayName("오늘 전체 시간과 선택 상태 책별 시간을 함께 조회한다")
        void 오늘전체시간과_선택상태_책별시간을_함께조회한다() {
            LocalDate today = LocalDate.of(2026, 3, 22);
            LibraryViewDto.UserStatusBookItem focusedBook = new LibraryViewDto.ReadingBookItem(
                    20L, "첫사랑의 침공", "권혁일", "https://cdn.nook.com/covers/focused.jpg", today
            );
            LibraryViewDto.UserStatusBookItem unfocusedBook = new LibraryViewDto.ReadingBookItem(
                    21L, "포커스 없는 책", "작가", "https://cdn.nook.com/covers/unfocused.jpg", today
            );
            CursorResponse<LibraryViewDto.UserStatusBookItem, Long> bookItems =
                    CursorResponse.of(List.of(focusedBook, unfocusedBook), 19L, true);
            given(focusRepository.findMonthlyFocusStats(user.getId(), today, today.plusDays(1)))
                    .willReturn(List.of(
                            new MonthlyFocusStatsDto(today, 20L, null, 120L),
                            new MonthlyFocusStatsDto(today, 30L, null, 50L),
                            new MonthlyFocusStatsDto(today, 21L, null, 0L)
                    ));
            given(libraryQueryService.getBooksByStatus(user.getId(), ReadingStatus.READING, 23L, 2))
                    .willReturn(new LibraryViewDto.StatusBookResponseDto(ReadingStatus.READING, bookItems));

            FocusResponseDto.HomeResponse response =
                    focusQueryService.getFocusHome(user, ReadingStatus.READING, 23L, 2);

            assertThat(response.todayFocusTime()).isEqualTo("00:02:50");
            assertThat(response.readingStatus()).isEqualTo(ReadingStatus.READING);
            assertThat(response.books().nextCursor()).isEqualTo(19L);
            assertThat(response.books().hasNext()).isTrue();
            assertThat(response.books().items()).containsExactly(
                    new FocusResponseDto.HomeBookItem(
                            20L, "첫사랑의 침공", "권혁일",
                            "https://cdn.nook.com/covers/focused.jpg", "00:02:00"
                    ),
                    new FocusResponseDto.HomeBookItem(
                            21L, "포커스 없는 책", "작가",
                            "https://cdn.nook.com/covers/unfocused.jpg", "00:00:00"
                    )
            );
            verify(focusRepository).findMonthlyFocusStats(user.getId(), today, today.plusDays(1));
            verify(libraryQueryService).getBooksByStatus(user.getId(), ReadingStatus.READING, 23L, 2);
            verifyNoInteractions(presignedUrlService);
        }

        @Test
        @DisplayName("집계와 책 목록이 비어 있거나 집계 시간이 null이면 0으로 반환한다")
        void 빈목록과_null집계시간은_0으로반환한다() {
            LocalDate today = LocalDate.of(2026, 3, 22);
            CursorResponse<LibraryViewDto.UserStatusBookItem, Long> emptyBookItems =
                    CursorResponse.of(List.of(), null, false);
            given(focusRepository.findMonthlyFocusStats(user.getId(), today, today.plusDays(1)))
                    .willReturn(List.of(new MonthlyFocusStatsDto(today, 20L, null, null)));
            given(libraryQueryService.getBooksByStatus(user.getId(), ReadingStatus.BEFORE, null, 20))
                    .willReturn(new LibraryViewDto.StatusBookResponseDto(ReadingStatus.BEFORE, emptyBookItems));

            FocusResponseDto.HomeResponse response =
                    focusQueryService.getFocusHome(user, ReadingStatus.BEFORE, null, 20);

            assertThat(response.todayFocusTime()).isEqualTo("00:00:00");
            assertThat(response.readingStatus()).isEqualTo(ReadingStatus.BEFORE);
            assertThat(response.books()).isEqualTo(
                    CursorResponse.of(List.of(), null, false)
            );
            verify(focusRepository).findMonthlyFocusStats(user.getId(), today, today.plusDays(1));
            verify(libraryQueryService).getBooksByStatus(user.getId(), ReadingStatus.BEFORE, null, 20);
            verifyNoInteractions(presignedUrlService);
        }

        @Test
        @DisplayName("집계와 책 목록이 모두 비어 있으면 0 시간과 빈 커서를 반환한다")
        void 빈집계와_빈책목록은_0시간과_빈커서를_반환한다() {
            LocalDate today = LocalDate.of(2026, 3, 22);
            CursorResponse<LibraryViewDto.UserStatusBookItem, Long> emptyBookItems =
                    CursorResponse.of(List.of(), null, false);
            given(focusRepository.findMonthlyFocusStats(user.getId(), today, today.plusDays(1)))
                    .willReturn(List.of());
            given(libraryQueryService.getBooksByStatus(user.getId(), ReadingStatus.FINISHED, null, 20))
                    .willReturn(new LibraryViewDto.StatusBookResponseDto(ReadingStatus.FINISHED, emptyBookItems));

            FocusResponseDto.HomeResponse response =
                    focusQueryService.getFocusHome(user, ReadingStatus.FINISHED, null, 20);

            assertThat(response).isEqualTo(new FocusResponseDto.HomeResponse(
                    "00:00:00",
                    ReadingStatus.FINISHED,
                    CursorResponse.of(List.of(), null, false)
            ));
            verify(focusRepository).findMonthlyFocusStats(user.getId(), today, today.plusDays(1));
            verify(libraryQueryService).getBooksByStatus(user.getId(), ReadingStatus.FINISHED, null, 20);
            verifyNoInteractions(presignedUrlService);
        }
    }
}
