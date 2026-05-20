package app.nook.focus.service;

import app.nook.focus.converter.FocusConverter;
import app.nook.focus.domain.Focus;
import app.nook.focus.dto.FocusResponseDto;
import app.nook.focus.repository.FocusRepository;
import app.nook.global.dto.CursorResponse;
import app.nook.r2.service.PresignedUrlService;
import app.nook.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FocusQueryService {

    private final FocusRepository focusRepository;
    private final PresignedUrlService presignedUrlService;

    public CursorResponse<FocusResponseDto.RecentFocusItem, Long> getRecentFocuses(
            User user,
            Long cursor,
            int size
    ) {
        Slice<Focus> slice = focusRepository.findRecentByUserWithCursor(
                user, cursor, PageRequest.of(0, size)
        );

        List<Focus> content = slice.getContent();
        Long nextCursor = slice.hasNext() ? content.get(content.size() - 1).getId() : null;

        List<FocusResponseDto.RecentFocusItem> items = content.stream()
                .map(focus -> FocusConverter.toRecentFocusItem(
                        focus,
                        presignedUrlService.resolveImageUrl(
                                user.getId(),
                                focus.getLibrary().getBook().getCoverImageKey()
                        )
                ))
                .toList();

        return CursorResponse.of(items, nextCursor, slice.hasNext());
    }
}
