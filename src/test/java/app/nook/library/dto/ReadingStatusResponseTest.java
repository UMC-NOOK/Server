package app.nook.library.dto;

import app.nook.library.domain.enums.ReadingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReadingStatusResponse 변환 테스트")
class ReadingStatusResponseTest {

    @Nested
    @DisplayName("from() 메서드")
    class From {

        @Test
        @DisplayName("BEFORE 상태를 변환하면 BEFORE를 반환한다")
        void from_BEFORE() {
            ReadingStatusResponse result = ReadingStatusResponse.from(ReadingStatus.BEFORE);

            assertThat(result).isEqualTo(ReadingStatusResponse.BEFORE);
        }

        @Test
        @DisplayName("READING 상태를 변환하면 READING을 반환한다")
        void from_READING() {
            ReadingStatusResponse result = ReadingStatusResponse.from(ReadingStatus.READING);

            assertThat(result).isEqualTo(ReadingStatusResponse.READING);
        }

        @Test
        @DisplayName("FINISHED 상태를 변환하면 FINISHED를 반환한다")
        void from_FINISHED() {
            ReadingStatusResponse result = ReadingStatusResponse.from(ReadingStatus.FINISHED);

            assertThat(result).isEqualTo(ReadingStatusResponse.FINISHED);
        }

        @ParameterizedTest
        @EnumSource(ReadingStatus.class)
        @DisplayName("모든 ReadingStatus 도메인 값에 대해 null이 아닌 값을 반환한다")
        void from_neverReturnsNull(ReadingStatus status) {
            ReadingStatusResponse result = ReadingStatusResponse.from(status);

            assertThat(result).isNotNull();
        }

        @ParameterizedTest
        @EnumSource(ReadingStatus.class)
        @DisplayName("from()은 UNREGISTERED를 반환하지 않는다")
        void from_neverReturnsUnregistered(ReadingStatus status) {
            ReadingStatusResponse result = ReadingStatusResponse.from(status);

            assertThat(result).isNotEqualTo(ReadingStatusResponse.UNREGISTERED);
        }

        @Test
        @DisplayName("모든 ReadingStatus 값이 대응되는 ReadingStatusResponse 값으로 변환된다")
        void from_allDomainStatusesMapped() {
            Set<ReadingStatusResponse> mapped = Arrays.stream(ReadingStatus.values())
                    .map(ReadingStatusResponse::from)
                    .collect(Collectors.toSet());

            assertThat(mapped).containsExactlyInAnyOrder(
                    ReadingStatusResponse.BEFORE,
                    ReadingStatusResponse.READING,
                    ReadingStatusResponse.FINISHED
            );
        }
    }

    @Nested
    @DisplayName("UNREGISTERED 상수")
    class Unregistered {

        @Test
        @DisplayName("UNREGISTERED는 유효한 열거형 상수이다")
        void unregistered_isValidEnumConstant() {
            assertThat(ReadingStatusResponse.UNREGISTERED).isNotNull();
            assertThat(ReadingStatusResponse.UNREGISTERED.name()).isEqualTo("UNREGISTERED");
        }

        @Test
        @DisplayName("UNREGISTERED는 from()으로 생성되지 않는 전용 상태이다")
        void unregistered_notProducedByFromMethod() {
            Set<ReadingStatusResponse> fromResults = Arrays.stream(ReadingStatus.values())
                    .map(ReadingStatusResponse::from)
                    .collect(Collectors.toSet());

            assertThat(fromResults).doesNotContain(ReadingStatusResponse.UNREGISTERED);
        }
    }

    @Nested
    @DisplayName("열거형 값 목록")
    class EnumValues {

        @Test
        @DisplayName("BEFORE, READING, FINISHED, UNREGISTERED 4개의 값을 가진다")
        void enumValues_containsAllExpectedConstants() {
            assertThat(ReadingStatusResponse.values())
                    .containsExactlyInAnyOrder(
                            ReadingStatusResponse.BEFORE,
                            ReadingStatusResponse.READING,
                            ReadingStatusResponse.FINISHED,
                            ReadingStatusResponse.UNREGISTERED
                    );
        }

        @Test
        @DisplayName("ReadingStatus보다 하나 더 많은 값을 가진다(UNREGISTERED 추가)")
        void enumValues_hasOneMoreValueThanDomainEnum() {
            assertThat(ReadingStatusResponse.values().length)
                    .isEqualTo(ReadingStatus.values().length + 1);
        }
    }
}