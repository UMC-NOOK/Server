package app.nook.record.service;

import app.nook.global.dto.CursorResponse;
import app.nook.r2.service.PresignedUrlService;
import app.nook.record.converter.RecordConverter;
import app.nook.record.domain.enums.SortType;
import app.nook.record.dto.BookRecordDto;
import app.nook.record.repository.RecordRepository;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecordViewService 테스트")
class RecordViewServiceTest {

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private RecordConverter recordConverter;

    @Mock
    private PresignedUrlService presignedUrlService;

    @InjectMocks
    private RecordViewService recordViewService;

    private User user() {
        return User.builder()
                .email("user@test.com")
                .nickName("user")
                .role(UserRole.USER)
                .provider("GOOGLE")
                .providerId("provider-1")
                .build();
    }

    @Nested
    @DisplayName("기록 목록 조회")
    class GetUserRecords {

        @Test
        @DisplayName("도서 표지 key를 응답용 URL로 변환한다")
        void getUserRecords_resolveCoverImageUrl() {
            User user = user();
            ReflectionTestUtils.setField(user, "id", 1L);

            BookRecordDto.BookRecordItemDto item = new BookRecordDto.BookRecordItemDto(
                    10L,
                    "테스트 도서",
                    "작가",
                    "최근 기록",
                    "book/users/1/cover.png",
                    3L,
                    LocalDateTime.of(2026, 4, 13, 10, 0)
            );
            given(recordRepository.findRecordsByCursor(1L, null, SortType.RECENT_RECORDED, 10))
                    .willReturn(List.of(item));
            given(presignedUrlService.resolveImageUrl(1L, "book/users/1/cover.png"))
                    .willReturn("https://r2.example.com/cover.png");

            CursorResponse<BookRecordDto.BookRecordItemDto, String> result =
                    recordViewService.getUserRecords(user, 10, null, SortType.RECENT_RECORDED);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).coverImageUrl())
                    .isEqualTo("https://r2.example.com/cover.png");
            assertThat(result.isHasNext()).isFalse();
            assertThat(result.getNextCursor()).isNull();
            verify(presignedUrlService).resolveImageUrl(1L, "book/users/1/cover.png");
        }
    }
}
