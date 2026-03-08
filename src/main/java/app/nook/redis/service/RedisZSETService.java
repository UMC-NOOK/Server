package app.nook.redis.service;

import app.nook.library.dto.FocusRankDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisZSETService {

    private static final DateTimeFormatter YYYYMM_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    // Redis 문자열 , ZSET 연산을 수행하는 템플릿
    private final StringRedisTemplate stringRedisTemplate;

    // 월별 책 통계를 Redis에서 읽어 DTO로 복원
    // 사용 출처: 포커스 완료 후 기록 생성
    public FocusRankDto.MonthlyBooksResponseDto loadMonthlyBooks(Long userId, YearMonth yearMonth) {
        // 일자별 ZSET key, 전체 카운트용 KV key
        String dayKey = zsetKey(userId, yearMonth);
        String totalKey = totalCountKey(userId, yearMonth);

        // 전체 책 수와 일자별 ZSET 멤버/score 조회
        String totalBookCountStr = stringRedisTemplate.opsForValue().get(totalKey);
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .rangeWithScores(dayKey, 0, -1);

        // 둘 중 하나라도 없으면 캐시 미스 처리
        if (totalBookCountStr == null || tuples == null || tuples.isEmpty()) {
            return null;
        }

        List<FocusRankDto.DailyBookItem> days = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            // 비정상 데이터 방어
            if (tuple.getValue() == null || tuple.getScore() == null) {
                continue;
            }
            // score는 epochDay로 저장했으므로 LocalDate로 복원
            LocalDate date = LocalDate.ofEpochDay(tuple.getScore().longValue());
            // member에 인코딩해 둔 day 집계 데이터 복원
            RedisDayRow decoded = decodeRedisMember(tuple.getValue());
            // topBook 정보가 있으면 DTO로 변환
            FocusRankDto.BookCalendarInfo topBook = decoded.topBookId() == null
                    ? null
                    : new FocusRankDto.BookCalendarInfo(decoded.topBookId(), decoded.topCoverUrl());
            days.add(new FocusRankDto.DailyBookItem(date, decoded.bookCount(), topBook));
        }

        // 최종 월별 응답 DTO 구성
        int totalBookCount = Integer.parseInt(totalBookCountStr);
        return new FocusRankDto.MonthlyBooksResponseDto(yearMonth, totalBookCount, days);
    }

    // 월별 책 통계를 Redis에 저장(ZSET + KV)
    public void saveMonthlyBooks(Long userId, YearMonth yearMonth, FocusRankDto.MonthlyBooksResponseDto response) {
        String dayKey = zsetKey(userId, yearMonth);
        String totalKey = totalCountKey(userId, yearMonth);

        // 재적재 시 기존 ZSET 데이터 초기화
        stringRedisTemplate.delete(dayKey);

        for (FocusRankDto.DailyBookItem day : response.days()) {
            // 날짜는 정렬 가능한 epochDay를 score로 사용
            long score = day.date().toEpochDay();
            // 멤버 문자열에는 day별 부가 데이터(bookCount/topBook)를 인코딩 저장
            String member = encodeRedisMember(day);
            stringRedisTemplate.opsForZSet().add(dayKey, member, score);
        }

        // 전체 책 수는 별도 KV로 저장
        stringRedisTemplate.opsForValue().set(totalKey, String.valueOf(response.totalBookCount()));

        // 월별 통계 캐시 TTL
        stringRedisTemplate.expire(dayKey, Duration.ofHours(24));
        stringRedisTemplate.expire(totalKey, Duration.ofHours(24));
    }

    // 월별 책 통계 캐시 무효화(ZSET + KV)
    public void evictMonthlyBooks(Long userId, YearMonth yearMonth) {
        stringRedisTemplate.delete(List.of(
                zsetKey(userId, yearMonth),
                totalCountKey(userId, yearMonth)
        ));
    }

    // 월별 일자별 포커스 시간 통계 저장(ZSET)
    public void saveMonthlyFocusTime(Long userId, YearMonth yearMonth, List<FocusRankDto.FocusTimeRow> rows) {
        String key = monthlyFocusTimeKey(userId, yearMonth);
        stringRedisTemplate.delete(key);

        for (FocusRankDto.FocusTimeRow row : rows) {
            if (row == null || row.date() == null || row.totalSec() == null) {
                continue;
            }
            // member = yyyy-MM-dd, score = totalSec
            stringRedisTemplate.opsForZSet().add(key, row.date().toString(), row.totalSec());
        }

        stringRedisTemplate.expire(key, Duration.ofHours(24));
    }

    // 월별 일자별 포커스 시간 통계 조회
    // 캐시 미스 시 null 반환
    public List<FocusRankDto.FocusTimeRow> loadMonthlyFocusTime(Long userId, YearMonth yearMonth) {
        String key = monthlyFocusTimeKey(userId, yearMonth);
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .rangeWithScores(key, 0, -1);

        if (tuples == null || tuples.isEmpty()) {
            return null;
        }

        return tuples.stream()
                .filter(tuple -> tuple.getValue() != null && tuple.getScore() != null)
                .map(tuple -> new FocusRankDto.FocusTimeRow(
                        LocalDate.parse(tuple.getValue()),
                        tuple.getScore().longValue()
                ))
                .sorted(Comparator.comparing(FocusRankDto.FocusTimeRow::date))
                .toList();
    }

    // 월별 일자별 포커스 시간 통계 무효화
    public void evictMonthlyFocusTime(Long userId, YearMonth yearMonth) {
        stringRedisTemplate.delete(monthlyFocusTimeKey(userId, yearMonth));
    }

    // DailyBookItem을 ZSET member 문자열로 직렬화
    private String encodeRedisMember(FocusRankDto.DailyBookItem day) {
        Long topBookId = day.topBook() == null ? null : day.topBook().bookId();
        String topCoverUrl = day.topBook() == null ? "" : safe(day.topBook().coverUrl());

        // URL은 구분자 충돌 방지를 위해 Base64(URL-safe) 인코딩
        String topCoverUrlEncoded = Base64.getUrlEncoder()
                .encodeToString(topCoverUrl.getBytes(StandardCharsets.UTF_8));

        // 형식: bookCount:topBookId(or null):base64CoverUrl
        return day.bookCount() + ":" + (topBookId == null ? "null" : topBookId) + ":" + topCoverUrlEncoded;
    }

    // ZSET member 문자열을 다시 도메인 값으로 역직렬화
    private RedisDayRow decodeRedisMember(String value) {
        String[] tokens = value.split(":", 3);
        long bookCount = Long.parseLong(tokens[0]);
        Long topBookId = "null".equals(tokens[1]) ? null : Long.parseLong(tokens[1]);
        // 저장 시 Base64 인코딩된 coverUrl 복원
        String coverUrl = new String(Base64.getUrlDecoder().decode(tokens[2]), StandardCharsets.UTF_8);
        if (coverUrl.isBlank()) {
            coverUrl = null;
        }
        return new RedisDayRow(bookCount, topBookId, coverUrl);
    }

    // 월별 책 통계 ZSET key 생성
    private String zsetKey(Long userId, YearMonth yearMonth) {
        return "perf:library:monthly:zset:" + userId + ":" + yearMonth;
    }

    // 월별 책 통계 total count KV key 생성
    private String totalCountKey(Long userId, YearMonth yearMonth) {
        return "perf:library:monthly:total:" + userId + ":" + yearMonth;
    }

    // null-safe 문자열 처리 유틸
    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record RedisDayRow(
            Long bookCount,
            Long topBookId,
            String topCoverUrl
    ) {
    }


    /**
     * 포커스 종료 시 해당 시간대 member의 score(totalSec)를 증가시킨다.
     * 내부적으로 Redis ZINCRBY 연산을 수행한다.
     * @param userId 사용자 ID
     * @param yearMonth 년월
     * @param hour 시간대
     * @param durationSec 집중한 초
     */
    public void incrementHourlyFocus(Long userId, YearMonth yearMonth, int hour, long durationSec) {
        // 잘못된 hour 입력을 조기에 차단
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("hour must be between 0 and 23");
        }
        // 0 이하 값은 통계에 반영하지 않음
        if (durationSec <= 0) {
            return;
        }

        String key = focusHourlyKey(userId, yearMonth);
        // 시간대 member는 고정 2자리 문자열("00"~"23")
        String member = String.format("%02d", hour);
        // score += durationSec
        stringRedisTemplate.opsForZSet().incrementScore(key, member, durationSec);
        // 월 단위 통계이므로 32일 TTL 부여
        stringRedisTemplate.expire(key, Duration.ofDays(32));
    }

    /**
     * 월별 시간대별 집중시간 조회.
     * Redis에서 전체 ZSET을 읽은 뒤 hour 오름차순(0->23)으로 정렬해 반환한다.
     * @return 시간대별 누적 초 리스트
     */
    public List<HourlyFocusStat> getMonthlyHourlyFocus(Long userId, YearMonth yearMonth) {
        String key = focusHourlyKey(userId, yearMonth);
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .rangeWithScores(key, 0, -1);

        // 캐시 미스 또는 데이터 없음
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }

        return tuples.stream()
                // 비정상 tuple 방어
                .filter(tuple -> tuple.getValue() != null && tuple.getScore() != null)
                .map(tuple -> new HourlyFocusStat(
                        Integer.parseInt(tuple.getValue()),
                        tuple.getScore().longValue()
                ))
                .sorted((a, b) -> Integer.compare(a.hour(), b.hour()))
                .toList();
    }

    /**
     * 월별 시간대 통계 key 전체 삭제.
     */
    public void evictMonthlyHourlyFocus(Long userId, YearMonth yearMonth) {
        stringRedisTemplate.delete(focusHourlyKey(userId, yearMonth));
    }

    // 시간대 통계 key 생성: stats:focus:hourly:{userId}:{yyyyMM}
    private String focusHourlyKey(Long userId, YearMonth yearMonth) {
        return "stats:focus:hourly:" + userId + ":" + yearMonth.format(YYYYMM_FORMATTER);
    }

    // 월별 일자별 포커스 시간 통계 key 생성: stats:focus:daily:{userId}:{yyyyMM}
    private String monthlyFocusTimeKey(Long userId, YearMonth yearMonth) {
        return "stats:focus:daily:" + userId + ":" + yearMonth.format(YYYYMM_FORMATTER);
    }

    public record HourlyFocusStat(
            int hour,
            long totalSec
    ) {
    }
}
