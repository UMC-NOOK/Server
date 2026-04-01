package app.nook.focus.service;

import app.nook.book.domain.Book;
import app.nook.focus.domain.Focus;
import app.nook.focus.domain.Theme;
import app.nook.focus.domain.enums.ThemeName;
import app.nook.focus.dto.FocusRequestDto;
import app.nook.focus.dto.FocusResponseDto;
import app.nook.focus.exception.FocusErrorCode;
import app.nook.focus.repository.FocusRepository;
import app.nook.focus.repository.ThemeRepository;
import app.nook.global.exception.CustomException;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.repository.LibraryRepository;
import app.nook.timeline.service.TimelineCommandService;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FocusServiceTest {

    @Mock
    private FocusRepository focusRepository;

    @Mock
    private LibraryRepository libraryRepository;

    @Mock
    private ThemeRepository themeRepository;

    @Mock
    private TimelineCommandService timelineCommandService;

    @InjectMocks
    private FocusService focusService;

    private User user;
    private Book book;
    private Library library;
    private Theme theme;
    private Focus focus;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .nickName("테스터")
                .provider("google")
                .providerId("provider-id")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        book = Book.builder()
                .title("첫사랑의 침공")
                .author("권혁일")
                .build();
        ReflectionTestUtils.setField(book, "id", 20L);

        library = Library.builder()
                .user(user)
                .book(book)
                .build();
        ReflectionTestUtils.setField(library, "id", 10L);
        ReflectionTestUtils.setField(library, "readingStatus", ReadingStatus.BEFORE);
        ReflectionTestUtils.setField(library, "focusSec", 0L);
        ReflectionTestUtils.setField(library, "page", 0);

        theme = Theme.builder()
                .name(ThemeName.THEME1)
                .imageUrl("https://cdn.nook.com/themes/theme1.png")
                .build();
        ReflectionTestUtils.setField(theme, "id", 1L);

        focus = Focus.builder()
                .library(library)
                .theme(theme)
                .startedAt(LocalDateTime.of(2026, 3, 22, 14, 0, 0))
                .endedAt(null)
                .durationSec(0)
                .build();
        ReflectionTestUtils.setField(focus, "id", 100L);
    }

    @Test
    @DisplayName("포커스 시작 성공")
    void startFocus_성공() {
        // given
        FocusRequestDto.FocusStart request = new FocusRequestDto.FocusStart(10L, 1L);

        given(focusRepository.findByLibraryUserIdAndEndedAtIsNull(user.getId()))
                .willReturn(Optional.empty());
        given(libraryRepository.findByIdAndUserId(10L, user.getId()))
                .willReturn(Optional.of(library));
        given(themeRepository.findById(1L))
                .willReturn(Optional.of(theme));
        given(focusRepository.save(any(Focus.class)))
                .willAnswer(invocation -> {
                    Focus saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", 100L);
                    return saved;
                });

        // when
        FocusResponseDto.FocusStart result = focusService.startFocus(user, request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.focusId()).isEqualTo(100L);
        assertThat(result.libraryId()).isEqualTo(10L);
        assertThat(result.bookId()).isEqualTo(20L);
        assertThat(result.bookTitle()).isEqualTo("첫사랑의 침공");
        assertThat(result.author()).isEqualTo("권혁일");
        assertThat(result.themeId()).isEqualTo(1L);
        assertThat(result.themeName()).isEqualTo("THEME1");
        assertThat(result.startedAt()).isNotNull();
        assertThat(library.getReadingStatus()).isEqualTo(ReadingStatus.READING);
    }

    @Test
    @DisplayName("포커스 시작 실패 - 이미 진행 중인 포커스가 있음")
    void startFocus_실패_이미진행중() {
        // given
        FocusRequestDto.FocusStart request = new FocusRequestDto.FocusStart(10L, 1L);

        given(focusRepository.findByLibraryUserIdAndEndedAtIsNull(user.getId()))
                .willReturn(Optional.of(focus));

        // when & then
        assertThatThrownBy(() -> focusService.startFocus(user, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(FocusErrorCode.FOCUS_ALREADY_IN_PROGRESS);
    }

    @Test
    @DisplayName("포커스 시작 실패 - 서재를 찾을 수 없음")
    void startFocus_실패_서재없음() {
        // given
        FocusRequestDto.FocusStart request = new FocusRequestDto.FocusStart(10L, 1L);

        given(focusRepository.findByLibraryUserIdAndEndedAtIsNull(user.getId()))
                .willReturn(Optional.empty());
        given(libraryRepository.findByIdAndUserId(10L, user.getId()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> focusService.startFocus(user, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(FocusErrorCode.LIBRARY_NOT_FOUND);
    }

    @Test
    @DisplayName("포커스 시작 실패 - 테마가 없음")
    void startFocus_실패_테마없음() {
        // given
        FocusRequestDto.FocusStart request = new FocusRequestDto.FocusStart(10L, 1L);

        given(focusRepository.findByLibraryUserIdAndEndedAtIsNull(user.getId()))
                .willReturn(Optional.empty());
        given(libraryRepository.findByIdAndUserId(10L, user.getId()))
                .willReturn(Optional.of(library));
        given(themeRepository.findById(1L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> focusService.startFocus(user, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(FocusErrorCode.THEME_NOT_FOUND);
    }

    @Test
    @DisplayName("포커스 종료 성공 - 완독 처리")
    void endFocus_성공_완독() {
        // given
        FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(100L, 72, true);

        given(focusRepository.findByIdAndLibraryUserId(100L, user.getId()))
                .willReturn(Optional.of(focus));

        // when
        FocusResponseDto.FocusEnd result = focusService.endFocus(user.getId(), request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.focusId()).isEqualTo(100L);
        assertThat(result.libraryId()).isEqualTo(10L);
        assertThat(result.startedAt()).isEqualTo(LocalDateTime.of(2026, 3, 22, 14, 0, 0));
        assertThat(result.endedAt()).isNotNull();
        assertThat(result.durationSec()).isGreaterThanOrEqualTo(0);
        assertThat(result.page()).isEqualTo(72);
        assertThat(focus.getEndPage()).isEqualTo(72);
        assertThat(result.totalFocusSec()).isGreaterThanOrEqualTo(0L);
        assertThat(result.readingStatus()).isEqualTo("FINISHED");
        verify(timelineCommandService).appendFocusCompleted(focus);
    }

    @Test
    @DisplayName("포커스 종료 성공 - 완독하지 않으면 READING 유지")
    void endFocus_성공_독서중유지() {
        // given
        FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(100L, 45, false);
        ReflectionTestUtils.setField(library, "readingStatus", ReadingStatus.READING);

        given(focusRepository.findByIdAndLibraryUserId(100L, user.getId()))
                .willReturn(Optional.of(focus));

        // when
        FocusResponseDto.FocusEnd result = focusService.endFocus(user.getId(), request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.page()).isEqualTo(45);
        assertThat(focus.getEndPage()).isEqualTo(45);
        assertThat(result.readingStatus()).isEqualTo("READING");
        verify(timelineCommandService).appendFocusCompleted(focus);
    }

    @Test
    @DisplayName("포커스 종료 실패 - 포커스를 찾을 수 없음")
    void endFocus_실패_포커스없음() {
        // given
        FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(100L, 72, false);

        given(focusRepository.findByIdAndLibraryUserId(100L, user.getId()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> focusService.endFocus(user.getId(), request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(FocusErrorCode.FOCUS_NOT_FOUND);
    }

    @Test
    @DisplayName("포커스 종료 실패 - 이미 종료된 포커스")
    void endFocus_실패_이미종료됨() {
        // given
        FocusRequestDto.FocusEnd request = new FocusRequestDto.FocusEnd(100L, 72, false);
        ReflectionTestUtils.setField(focus, "endedAt", LocalDateTime.of(2026, 3, 22, 14, 34, 26));

        given(focusRepository.findByIdAndLibraryUserId(100L, user.getId()))
                .willReturn(Optional.of(focus));

        // when & then
        assertThatThrownBy(() -> focusService.endFocus(user.getId(), request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(FocusErrorCode.FOCUS_ALREADY_ENDED);
    }
}
