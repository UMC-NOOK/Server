package app.nook.library.service;

import app.nook.focus.domain.Focus;
import app.nook.focus.repository.FocusRepository;
import app.nook.focus.service.FocusDailyTimeCalculator;
import app.nook.global.dto.CursorResponse;
import app.nook.global.exception.CustomException;
import app.nook.global.response.AuthErrorCode;
import app.nook.library.converter.LibraryConverter;
import app.nook.library.domain.Library;
import app.nook.library.domain.enums.LibrarySortType;
import app.nook.library.domain.enums.ReadingStatus;
import app.nook.library.dto.LibraryBookCursor;
import app.nook.library.dto.LibraryViewDto;
import app.nook.library.repository.LibraryRepository;
import app.nook.library.repository.dto.LibraryBookQueryResult;
import app.nook.library.util.FocusTimeUtil;
import app.nook.library.util.LibraryBookCursorCodec;
import app.nook.r2.service.PresignedUrlService;
import app.nook.user.domain.User;
import app.nook.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LibraryQueryService {

    private static final int LIBRARY_SEARCH_HOME_MAX_SIZE = 5;

    private final LibraryRepository libraryRepository;
    private final FocusRepository focusRepository;
    private final PresignedUrlService presignedUrlService;
    private final UserRepository userRepository;
    private final FocusDailyTimeCalculator focusDailyTimeCalculator;
    private final Clock clock;

    public LibraryViewDto.BookCountResponseDto getBookCount(Long userId) {
        return new LibraryViewDto.BookCountResponseDto(libraryRepository.countByUserId(userId));
    }

    public LibraryViewDto.StatusBookResponseDto getBooksByStatus(
            Long userId,
            ReadingStatus status,
            Long cursor,
            int size
    ) {
        // 다음 페이지 존재 여부를 확인하기 위해 한 건 더 조회
        Pageable pageable = PageRequest.of(0, size + 1);

        Slice<Library> libraries = libraryRepository.findByUserIdAndStatusWithCursor(
                userId,
                status,
                cursor,
                pageable
        );

        // 응답 직전에 커버 이미지를 실제 조회 URL로 치환
        CursorResponse<LibraryViewDto.UserStatusBookItem, Long> cursorResponse =
                toResolvedCursorResponse(userId, libraries.getContent(), size);

        int totalCount = 0;
        if (cursor == null) {
            // 첫 페이지에서만 전체 개수를 함께 내려준다
            totalCount = (int) libraryRepository.countByUserIdAndReadingStatus(userId, status);
        }

        return LibraryConverter.toStatusBookResponse(status, totalCount, cursorResponse);
    }

    public Set<String> getOwnedIsbns(Long userId, List<String> isbns) {
        if (isbns.isEmpty()) {
            return Set.of();
        }
        return libraryRepository.findAladinIsbnsByUserIdAndIsbnIn(userId, isbns);
    }

    public Page<Library> searchBooksInLibrary(Long userId, String keyword, int page, int size) {
        String escapedKeyword = keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        Pageable pageable = PageRequest.of(page, size);
        return libraryRepository.searchByUserIdAndKeyword(userId, escapedKeyword, pageable);
    }

    public CursorResponse<LibraryViewDto.UserBookResponseDto, Long> getFocusRecordsByDate(
            Long userId,
            LocalDate date,
            Long cursor,
            int size
    ) {
        // 날짜별 포커스 기록도 커서 기반으로 한 건 더 조회한다
        User user = getUser(userId);
        Pageable pageable = PageRequest.of(0, size + 1);
        LocalDateTime serverNow = LocalDateTime.now(clock);

        Slice<Focus> focuses = focusRepository.findByLibraryWithCursorByDate(
                user,
                date,
                serverNow,
                cursor,
                pageable
        );

        List<Focus> content = focuses.getContent();
        boolean hasNext = content.size() > size;
        List<Focus> pageContent = hasNext ? content.subList(0, size) : content;

        // 응답 전 책 정보와 이미지 URL을 조회용 형태로 정리
        List<LibraryViewDto.UserBookResponseDto> bookItems = pageContent.stream()
                .map(focus -> new LibraryViewDto.UserBookResponseDto(
                        focus.getLibrary().getBook().getId(),
                        focus.getLibrary().getBook().getTitle(),
                        focus.getLibrary().getBook().getAuthor(),
                        FocusTimeUtil.formatFocusTime(Math.toIntExact(focusDailyTimeCalculator.calculateForDate(
                                focus.getStartedAt(),
                                focus.getEndedAt(),
                                serverNow,
                                date
                        ))),
                        presignedUrlService.resolveImageUrl(userId, focus.getLibrary().getBook().getCoverImageKey())
                ))
                .toList();

        Long nextCursor = hasNext ? pageContent.get(pageContent.size() - 1).getId() : null;
        return CursorResponse.of(bookItems, nextCursor, hasNext);
    }

    public LibraryViewDto.BeforeReadingResponseDto getBeforeReadingBooks(Long userId) {
        // 홈 진입용으로 읽기 전 상태 도서를 최대 5권만 노출
        List<LibraryViewDto.BeforeBookItem> books = libraryRepository
                .findByUserIdAndReadingStatusOrderByIdDesc(
                        userId,
                        ReadingStatus.BEFORE,
                        PageRequest.of(0, 5)
                )
                .stream()
                .map(library -> new LibraryViewDto.BeforeBookItem(
                        library.getBook().getId(),
                        library.getBook().getTitle(),
                        library.getBook().getAuthor(),
                        presignedUrlService.resolveImageUrl(userId, library.getBook().getCoverImageKey())
                ))
                .toList();

        return new LibraryViewDto.BeforeReadingResponseDto(books);
    }

    public LibraryViewDto.RecentFocusResponseDto getRecentFocus(Long userId) {
        User user = getUser(userId);
        return focusRepository.findRecentByUser(user, PageRequest.of(0, 1)).stream().findFirst()
                .map(focus -> {
                    // 0페이지는 미기록 상태로 간주해 null로 응답
                    int currentPage = focus.getLibrary().getPage();
                    Integer page = currentPage == 0 ? null : currentPage;
                    return new LibraryViewDto.RecentFocusResponseDto(
                            focus.getLibrary().getBook().getId(),
                            presignedUrlService.resolveImageUrl(userId, focus.getLibrary().getBook().getCoverImageKey()),
                            focus.getLibrary().getBook().getTitle(),
                            page,
                            FocusTimeUtil.formatFocusTime(focus.getDurationSec() == null ? 0 : focus.getDurationSec())
                    );
                })
                .orElse(null);
    }

    public List<LibraryViewDto.RecentFocusBookItem> getRecentFocusBooks(Long userId, int size) {
        User user = getUser(userId);
        int pageSize = Math.min(Math.max(size, 1), LIBRARY_SEARCH_HOME_MAX_SIZE);

        return focusRepository.findRecentDistinctBooksByUser(user, PageRequest.of(0, pageSize)).stream()
                .map(focus -> new LibraryViewDto.RecentFocusBookItem(
                        focus.getLibrary().getBook().getId(),
                        focus.getLibrary().getBook().getTitle(),
                        focus.getLibrary().getBook().getAuthor(),
                        presignedUrlService.resolveImageUrl(userId, focus.getLibrary().getBook().getCoverImageKey())
                ))
                .toList();
    }

    public LibraryViewDto.YearResponseDto getReadingYears(Long userId) {
        User user = getUser(userId);
        // 가입 연도부터 현재 연도까지 연속된 선택 범위를 만든다
        int startYear = user.getCreatedDate().getYear();
        int currentYear = LocalDateTime.now().getYear();
        List<Integer> years = IntStream.rangeClosed(startYear, currentYear)
                .boxed()
                .collect(Collectors.toCollection(ArrayList::new));
        return new LibraryViewDto.YearResponseDto(years);
    }

    private CursorResponse<LibraryViewDto.UserStatusBookItem, Long> toResolvedCursorResponse(
            Long userId,
            List<Library> libraries,
            int size
    ) {
        // 커서 계산은 유지하고, 아이템만 조회 가능한 URL로 치환
        CursorResponse<LibraryViewDto.UserStatusBookItem, Long> cursorResponse =
                LibraryConverter.toCursorResponse(libraries, size);
        List<LibraryViewDto.UserStatusBookItem> resolvedItems = cursorResponse.items().stream()
                .map(item -> resolveStatusBookItem(userId, item))
                .toList();
        return CursorResponse.of(resolvedItems, cursorResponse.nextCursor(), cursorResponse.hasNext());
    }

    private LibraryViewDto.UserStatusBookItem resolveStatusBookItem(Long userId, LibraryViewDto.UserStatusBookItem item) {
        String resolvedCoverUrl = presignedUrlService.resolveImageUrl(userId, item.coverUrl());
        if (item instanceof LibraryViewDto.BeforeBookItem before) {
            return new LibraryViewDto.BeforeBookItem(before.bookId(), before.title(), before.author(), resolvedCoverUrl);
        }
        if (item instanceof LibraryViewDto.ReadingBookItem reading) {
            return new LibraryViewDto.ReadingBookItem(
                    reading.bookId(),
                    reading.title(),
                    reading.author(),
                    resolvedCoverUrl,
                    reading.startedAt()
            );
        }
        LibraryViewDto.FinishedBookItem finished = (LibraryViewDto.FinishedBookItem) item;
        return new LibraryViewDto.FinishedBookItem(
                finished.bookId(),
                finished.title(),
                finished.author(),
                resolvedCoverUrl,
                finished.startedAt(),
                finished.endedAt()
        );
    }

    // 서재 전체 책 목록 조회 (무한스크롤)
    public CursorResponse<LibraryViewDto.LibraryBookItem, String> getAllBooks(
            Long userId, LibraryBookCursor cursor, LibrarySortType sortType, int size
    ) {
        List<LibraryBookQueryResult> results = libraryRepository.findAllBooksByCursor(userId, cursor, sortType, size);

        boolean hasNext = results.size() > size;
        List<LibraryBookQueryResult> pageContent = hasNext ? results.subList(0, size) : results;

        LibraryBookCursor nextCursor = hasNext ? toNextCursor(pageContent.get(pageContent.size() - 1), sortType) : null;

        List<LibraryViewDto.LibraryBookItem> items = pageContent.stream()
                .map(r -> new LibraryViewDto.LibraryBookItem(
                        r.bookId(),
                        r.title(),
                        r.author(),
                        presignedUrlService.resolveImageUrl(userId, r.coverImageKey()),
                        r.readingStatus()
                ))
                .toList();

        return CursorResponse.of(items, LibraryBookCursorCodec.encode(nextCursor), hasNext);
    }

    private LibraryBookCursor toNextCursor(LibraryBookQueryResult last, LibrarySortType sortType) {
        return switch (sortType) {
            case RECENT_FOCUSED -> new LibraryBookCursor(last.libraryId(), last.lastFocusedAt(), null, null);
            case RECORD_COUNT_DESC, RECORD_COUNT_ASC -> new LibraryBookCursor(last.libraryId(), null, last.recordCount(), null);
            case ALPHABETICAL -> new LibraryBookCursor(last.libraryId(), null, null, last.title());
        };
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }
}
