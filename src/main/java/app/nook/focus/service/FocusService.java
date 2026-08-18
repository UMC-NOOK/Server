package app.nook.focus.service;

import app.nook.focus.converter.FocusConverter;
import app.nook.focus.domain.Focus;
import app.nook.focus.domain.Theme;
import app.nook.focus.dto.FocusRequestDto;
import app.nook.focus.dto.FocusResponseDto;
import app.nook.focus.exception.FocusErrorCode;
import app.nook.focus.repository.FocusRepository;
import app.nook.focus.repository.ThemeRepository;
import app.nook.global.exception.CustomException;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.event.LibraryCacheInvalidateEvent;
import app.nook.library.repository.LibraryRepository;
import app.nook.timeline.service.TimelineCommandService;
import app.nook.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class FocusService {

    private final FocusRepository focusRepository;
    private final LibraryRepository libraryRepository;
    private final ThemeRepository themeRepository;
    private final TimelineCommandService timelineCommandService;
    private final ApplicationEventPublisher eventPublisher;
    private final FocusDailyTimeCalculator focusDailyTimeCalculator;
    private final Clock clock;

    public FocusResponseDto.FocusStart startFocus(User user, FocusRequestDto.FocusStart request) {

        // 1. 이미 진행 중인 포커스가 있는지 확인
        focusRepository.findByLibraryUserIdAndEndedAtIsNull(user.getId())
                .ifPresent(focus -> {
                    throw new CustomException(FocusErrorCode.FOCUS_ALREADY_IN_PROGRESS);
                });

        // 2. 내 서재 책인지 확인
        Library library = libraryRepository.findByIdAndUserId(request.libraryId(), user.getId())
                .orElseThrow(() -> new CustomException(FocusErrorCode.LIBRARY_NOT_FOUND));

        // 3. 테마 존재 여부 확인
        Theme theme = themeRepository.findById(request.themeId())
                .orElseThrow(() -> new CustomException(FocusErrorCode.THEME_NOT_FOUND));

        // 4. Focus 생성
        Focus focus = Focus.builder()
                .library(library)
                .theme(theme)
                .startedAt(LocalDateTime.now(clock))
                .endedAt(null)
                .durationSec(0)
                .build();

        Focus savedFocus = focusRepository.save(focus);

        if (library.getReadingStatus() == ReadingStatus.BEFORE) {
            library.updateStatus(ReadingStatus.READING);
        }

        return FocusConverter.toFocusStartResponse(savedFocus);
    }

    public FocusResponseDto.FocusEnd endFocus(Long userId, FocusRequestDto.FocusEnd request) {

        Focus focus = focusRepository.findByIdAndLibraryUserId(request.focusId(), userId)
                .orElseThrow(() -> new CustomException(FocusErrorCode.FOCUS_NOT_FOUND));

        if (focus.getEndedAt() != null) {
            throw new CustomException(FocusErrorCode.FOCUS_ALREADY_ENDED);
        }

        LocalDateTime endedAt = LocalDateTime.now(clock);
        focus.endFocus(endedAt, request.page());

        Library library = focus.getLibrary();
        library.recordFocus(focus.getDurationSec());
        library.recordPage(request.page());

        boolean isFinished = Boolean.TRUE.equals(request.isFinished());
        if (isFinished) {
            library.updateStatus(ReadingStatus.FINISHED);
        } else if (library.getReadingStatus() == ReadingStatus.BEFORE) {
            library.updateStatus(ReadingStatus.READING);
        }

        Set<YearMonth> affectedYearMonths = affectedYearMonths(focus.getStartedAt(), endedAt);
        eventPublisher.publishEvent(isFinished
                ? LibraryCacheInvalidateEvent.monthlyAndOnboardingGoal(userId, affectedYearMonths)
                : LibraryCacheInvalidateEvent.monthly(userId, affectedYearMonths));

        timelineCommandService.appendFocusCompleted(focus);

        return FocusConverter.toFocusEndResponse(focus);
    }

    private Set<YearMonth> affectedYearMonths(LocalDateTime startedAt, LocalDateTime endedAt) {
        return focusDailyTimeCalculator.affectedYearMonths(startedAt, endedAt, endedAt);
    }
}
