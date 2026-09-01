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
import app.nook.timeline.event.FocusTimelineAppendEvent;
import app.nook.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class FocusService {

    private final FocusRepository focusRepository;
    private final LibraryRepository libraryRepository;
    private final ThemeRepository themeRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final FocusCompletionSegmenter focusCompletionSegmenter;

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
        LocalDateTime startedAt = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        Focus focus = Focus.builder()
                .library(library)
                .theme(theme)
                .startedAt(startedAt)
                .endedAt(null)
                .durationSec(0)
                .build();

        Focus savedFocus = focusRepository.save(focus);

        if (library.getReadingStatus() == ReadingStatus.BEFORE) {
            library.updateStatus(ReadingStatus.READING, startedAt.toLocalDate());
        }

        return FocusConverter.toFocusStartResponse(savedFocus);
    }

    public FocusResponseDto.FocusEnd endFocus(Long userId, FocusRequestDto.FocusEnd request) {

        Focus focus = focusRepository.findByIdAndLibraryUserIdForUpdate(request.focusId(), userId)
                .orElseThrow(() -> new CustomException(FocusErrorCode.FOCUS_NOT_FOUND));

        if (focus.getEndedAt() != null) {
            throw new CustomException(FocusErrorCode.FOCUS_ALREADY_ENDED);
        }

        LocalDateTime endedAt = LocalDateTime.now(clock);
        List<FocusCompletionSegmenter.CompletedFocusSegment> segments =
                focusCompletionSegmenter.split(focus.getStartedAt(), endedAt);
        if (segments.isEmpty()) {
            throw new IllegalStateException("Focus end time precedes start time");
        }

        LocalDateTime normalizedStartedAt = segments.get(0).startedAt();
        LocalDateTime normalizedEndedAt = segments.get(segments.size() - 1).endedAt();
        int totalDurationSec = Math.toIntExact(segments.stream()
                .mapToLong(FocusCompletionSegmenter.CompletedFocusSegment::durationSec)
                .sum());
        Set<YearMonth> affectedYearMonths = new LinkedHashSet<>();
        YearMonth finalAffectedMonth = normalizedStartedAt.equals(normalizedEndedAt)
                ? YearMonth.from(normalizedStartedAt)
                : YearMonth.from(normalizedEndedAt.minusNanos(1));
        for (YearMonth month = YearMonth.from(normalizedStartedAt);
             !month.isAfter(finalAffectedMonth);
             month = month.plusMonths(1)) {
            affectedYearMonths.add(month);
        }

        Library library = focus.getLibrary();
        Theme theme = focus.getTheme();
        List<Focus> completedFocuses = new ArrayList<>(segments.size());
        for (int index = 0; index < segments.size(); index++) {
            FocusCompletionSegmenter.CompletedFocusSegment segment = segments.get(index);
            Integer endPage = index == segments.size() - 1 ? request.page() : null;
            if (index == 0) {
                focus.completeSegment(segment.startedAt(), segment.endedAt(), endPage);
                completedFocuses.add(focus);
            } else {
                completedFocuses.add(Focus.builder()
                        .library(library)
                        .theme(theme)
                        .startedAt(segment.startedAt())
                        .endedAt(segment.endedAt())
                        .durationSec(Math.toIntExact(segment.durationSec()))
                        .endPage(endPage)
                        .build());
            }
        }

        library.recordFocus(totalDurationSec);
        library.recordPage(request.page());

        if (Boolean.TRUE.equals(request.isFinished())) {
            library.updateStatus(ReadingStatus.FINISHED, normalizedEndedAt.toLocalDate());
        } else if (library.getReadingStatus() == ReadingStatus.BEFORE) {
            library.updateStatus(ReadingStatus.READING, normalizedEndedAt.toLocalDate());
        }

        List<Focus> savedFocuses = focusRepository.saveAllAndFlush(completedFocuses);
        for (Focus savedFocus : savedFocuses) {
            if (savedFocus.getId() == null) {
                throw new IllegalStateException("Completed Focus ID must be generated before Timeline creation");
            }
        }
        eventPublisher.publishEvent(new FocusTimelineAppendEvent(savedFocuses.stream()
                .map(Focus::getId)
                .toList()));

        eventPublisher.publishEvent(Boolean.TRUE.equals(request.isFinished())
                ? LibraryCacheInvalidateEvent.monthlyAndOnboardingGoal(userId, affectedYearMonths)
                : LibraryCacheInvalidateEvent.monthly(userId, affectedYearMonths));

        return FocusConverter.toFocusEndResponse(
                focus,
                normalizedStartedAt,
                normalizedEndedAt,
                totalDurationSec
        );
    }
}
