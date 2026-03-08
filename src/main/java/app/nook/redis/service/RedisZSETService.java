package app.nook.redis.service;

import app.nook.library.dto.FocusRankDto;
import app.nook.redis.dto.HourlyFocusStat;
import app.nook.redis.dto.MonthlyBookCacheRow;
import app.nook.redis.dto.RedisDayRow;
import app.nook.redis.dto.ScoredRedisDayRow;
import app.nook.redis.exception.RedisErrorCode;
import app.nook.redis.exception.RedisOperationException;
import app.nook.redis.util.RedisMonthlyBookMemberCodec;
import app.nook.redis.util.RedisStatsKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisZSETService {
    private final StringRedisTemplate stringRedisTemplate;

    // 월별 책 통계를 캐시에서 조회
    public FocusRankDto.MonthlyBooksResponseDto loadMonthlyBooks(Long userId, YearMonth yearMonth) {
        return executeRead(() -> {
            String dayKey = zsetKey(userId, yearMonth);
            String totalKey = totalCountKey(userId, yearMonth);
            String existsKey = monthlyBooksExistsKey(userId, yearMonth);

            Boolean exists = stringRedisTemplate.hasKey(existsKey);
            // exists 없다면 캐시 미스
            if (!Boolean.TRUE.equals(exists)) {
                return null;
            }

            String totalBookCountStr = stringRedisTemplate.opsForValue().get(totalKey);
            Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                    .reverseRangeWithScores(dayKey, 0, -1);

            if (tuples == null || tuples.isEmpty()) {
                int totalBookCount = parseIntegerOrDefault(totalBookCountStr, 0, totalKey);
                return new FocusRankDto.MonthlyBooksResponseDto(yearMonth, totalBookCount, List.of());
            }

            if (totalBookCountStr == null) {
                return null;
            }

            List<ScoredRedisDayRow> scoredRows = new ArrayList<>();

            // ZSET에서 읽은 데이터를 디코딩하여 점수와 함께 리스트에 저장
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                if (tuple.getValue() == null || tuple.getScore() == null) {
                    continue;
                }
                RedisDayRow decoded;
                try {
                    decoded = RedisMonthlyBookMemberCodec.decode(tuple.getValue());
                } catch (RuntimeException e) {
                    log.warn("월별 책 캐시 멤버 형식 오류로 건너뜁니다. key={}, member={}", dayKey, tuple.getValue());
                    continue;
                }
                scoredRows.add(new ScoredRedisDayRow(tuple.getScore(), decoded));
            }

            List<FocusRankDto.DailyBookItem> days = scoredRows.stream()
                    .sorted(Comparator.comparingDouble(ScoredRedisDayRow::score).reversed()
                            .thenComparing(item -> item.row().date()))
                    .map(item -> {
                        RedisDayRow row = item.row();
                        FocusRankDto.BookCalendarInfo topBook = row.topBookId() == null
                                ? null
                                : new FocusRankDto.BookCalendarInfo(row.topBookId(), row.topCoverUrl());
                        return new FocusRankDto.DailyBookItem(row.date(), row.bookCount(), topBook);
                    })
                    .toList();

            int totalBookCount = parseIntegerOrDefault(totalBookCountStr, 0, totalKey);
            return new FocusRankDto.MonthlyBooksResponseDto(yearMonth, totalBookCount, days);
        });
    }

    // 월별 책 통계를 응답 DTO 기준으로 캐시에 저장
    public void saveMonthlyBooks(Long userId, YearMonth yearMonth, FocusRankDto.MonthlyBooksResponseDto response) {
        executeWrite(() -> {
            String dayKey = zsetKey(userId, yearMonth);
            String totalKey = totalCountKey(userId, yearMonth);
            String existsKey = monthlyBooksExistsKey(userId, yearMonth);

            // 갱신 중에는 캐시 미스로 처리되도록 존재 마커를 먼저 제거
            stringRedisTemplate.delete(existsKey);
            stringRedisTemplate.delete(dayKey);

            for (int i = 0; i < response.days().size(); i++) {
                FocusRankDto.DailyBookItem day = response.days().get(i);
                // 입력 순서를 유지하도록 순위 점수 저장
                long score = response.days().size() - i;
                String member = RedisMonthlyBookMemberCodec.encode(
                        day.date(),
                        day.bookCount(),
                        day.topBook() == null ? null : day.topBook().bookId(),
                        day.topBook() == null ? null : day.topBook().coverUrl()
                );
                stringRedisTemplate.opsForZSet().add(dayKey, member, score);
            }

            stringRedisTemplate.opsForValue().set(totalKey, String.valueOf(response.totalBookCount()));
            stringRedisTemplate.opsForValue().set(existsKey, "1");
            stringRedisTemplate.expire(dayKey, Duration.ofHours(24));
            stringRedisTemplate.expire(totalKey, Duration.ofHours(24));
            stringRedisTemplate.expire(existsKey, Duration.ofHours(24));
        });
    }

    // 월별 책 통계를 정렬 점수 포함 행 기준으로 캐시에 저장
    public void saveMonthlyBooks(
            Long userId,
            YearMonth yearMonth,
            int totalBookCount,
            List<MonthlyBookCacheRow> rows
    ) {
        executeWrite(() -> {
            String dayKey = zsetKey(userId, yearMonth);
            String totalKey = totalCountKey(userId, yearMonth);
            String existsKey = monthlyBooksExistsKey(userId, yearMonth);

            // 갱신 중에는 캐시 미스로 처리되도록 존재 마커를 먼저 제거
            stringRedisTemplate.delete(existsKey);
            stringRedisTemplate.delete(dayKey);

            for (MonthlyBookCacheRow row : rows) {
                if (row == null || row.date() == null) {
                    continue;
                }
                String member = RedisMonthlyBookMemberCodec.encode(
                        row.date(),
                        row.bookCount(),
                        row.topBookId(),
                        row.topCoverUrl()
                );
                stringRedisTemplate.opsForZSet().add(dayKey, member, row.topFocusSec());
            }

            stringRedisTemplate.opsForValue().set(totalKey, String.valueOf(totalBookCount));
            stringRedisTemplate.opsForValue().set(existsKey, "1");
            stringRedisTemplate.expire(dayKey, Duration.ofHours(24));
            stringRedisTemplate.expire(totalKey, Duration.ofHours(24));
            stringRedisTemplate.expire(existsKey, Duration.ofHours(24));
        });
    }

    // 월별 책 통계 캐시 키를 삭제
    public void evictMonthlyBooks(Long userId, YearMonth yearMonth) {
        executeWrite(() -> stringRedisTemplate.delete(List.of(
                zsetKey(userId, yearMonth),
                totalCountKey(userId, yearMonth),
                monthlyBooksExistsKey(userId, yearMonth)
        )));
    }

    // 월별 일자 포커스 통계를 캐시에 저장
    public void saveMonthlyFocusTime(Long userId, YearMonth yearMonth, List<FocusRankDto.FocusTimeRow> rows) {
        executeWrite(() -> {
            String key = monthlyFocusTimeKey(userId, yearMonth);
            String existsKey = monthlyFocusTimeExistsKey(userId, yearMonth);
            // 갱신 중에는 캐시 미스로 처리되도록 존재 마커를 먼저 제거
            stringRedisTemplate.delete(existsKey);
            stringRedisTemplate.delete(key);

            for (FocusRankDto.FocusTimeRow row : rows) {
                if (row == null || row.date() == null || row.totalSec() == null) {
                    continue;
                }
                stringRedisTemplate.opsForZSet().add(key, row.date().toString(), row.totalSec());
            }

            stringRedisTemplate.opsForValue().set(existsKey, "1");
            stringRedisTemplate.expire(key, Duration.ofHours(24));
            stringRedisTemplate.expire(existsKey, Duration.ofHours(24));
        });
    }

    // 월별 일자 포커스 통계를 캐시에서 조회
    public List<FocusRankDto.FocusTimeRow> loadMonthlyFocusTime(Long userId, YearMonth yearMonth) {
        return executeRead(() -> {
            String key = monthlyFocusTimeKey(userId, yearMonth);
            String existsKey = monthlyFocusTimeExistsKey(userId, yearMonth);
            Boolean exists = stringRedisTemplate.hasKey(existsKey);
            if (!Boolean.TRUE.equals(exists)) {
                return null;
            }
            Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                    .rangeWithScores(key, 0, -1);

            if (tuples == null || tuples.isEmpty()) {
                return List.of();
            }

            List<FocusRankDto.FocusTimeRow> rows = new ArrayList<>();
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                if (tuple.getValue() == null || tuple.getScore() == null) {
                    continue;
                }
                try {
                    rows.add(new FocusRankDto.FocusTimeRow(
                            LocalDate.parse(tuple.getValue()),
                            tuple.getScore().longValue()
                    ));
                } catch (RuntimeException e) {
                    log.warn("[HOME][FOCUS] 월별 포커스 시간 캐시 멤버 형식 오류로 건너뜀 key={}, member={}", key, tuple.getValue());
                }
            }
            return rows.stream()
                    .sorted(Comparator.comparing(FocusRankDto.FocusTimeRow::date))
                    .toList();
        });
    }

    // 월별 일자 포커스 통계 캐시 키를 삭제
    public void evictMonthlyFocusTime(Long userId, YearMonth yearMonth) {
        executeWrite(() -> stringRedisTemplate.delete(List.of(
                monthlyFocusTimeKey(userId, yearMonth),
                monthlyFocusTimeExistsKey(userId, yearMonth)
        )));
    }

    // 월별 책 통계 ZSET 키를 생성
    private String zsetKey(Long userId, YearMonth yearMonth) {
        return RedisStatsKeyUtil.monthlyBooksZsetKey(userId, yearMonth);
    }

    // 월별 책 통계 총개수 키를 생성
    private String totalCountKey(Long userId, YearMonth yearMonth) {
        return RedisStatsKeyUtil.monthlyBooksTotalKey(userId, yearMonth);
    }

    // 월별 책 통계 존재 마커 키를 생성
    private String monthlyBooksExistsKey(Long userId, YearMonth yearMonth) {
        return RedisStatsKeyUtil.monthlyBooksExistsKey(userId, yearMonth);
    }

    // 포커스 시간 증가
    // 포커스 기록 생성 시 호출
    public void incrementHourlyFocus(Long userId, YearMonth yearMonth, int hour, long durationSec) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("hour must be between 0 and 23");
        }
        if (durationSec <= 0) {
            return;
        }

        executeWrite(() -> {
            String key = focusHourlyKey(userId, yearMonth);
            String member = String.format("%02d", hour);
            stringRedisTemplate.opsForZSet().incrementScore(key, member, durationSec);
            stringRedisTemplate.expire(key, Duration.ofDays(32));
        });
    }


    // 월별 시간대 포커스 통계를 캐시에서 조회
    // 포커스 기록 생성 시 호출
    public List<HourlyFocusStat> getMonthlyHourlyFocus(Long userId, YearMonth yearMonth) {
        return executeRead(() -> {
            String key = focusHourlyKey(userId, yearMonth);
            Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                    .rangeWithScores(key, 0, -1);

            if (tuples == null || tuples.isEmpty()) {
                return List.of();
            }

            List<HourlyFocusStat> rows = new ArrayList<>();
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                if (tuple.getValue() == null || tuple.getScore() == null) {
                    continue;
                }
                try {
                    rows.add(new HourlyFocusStat(
                            Integer.parseInt(tuple.getValue()),
                            tuple.getScore().longValue()
                    ));
                } catch (RuntimeException e) {
                    log.warn("[HOME][FOCUS] 시간대 포커스 캐시 멤버 형식 오류로 건너뜀 key={}, member={}", key, tuple.getValue());
                }
            }
            return rows.stream()
                    .sorted((a, b) -> Integer.compare(a.hour(), b.hour()))
                    .toList();
        });
    }

    // 포커스 시간 캐시 무효화
    public void evictMonthlyHourlyFocus(Long userId, YearMonth yearMonth) {
        executeWrite(() -> stringRedisTemplate.delete(focusHourlyKey(userId, yearMonth)));
    }

    // hourly 포커스 시간 캐시 키
    private String focusHourlyKey(Long userId, YearMonth yearMonth) {
        return RedisStatsKeyUtil.monthlyFocusHourlyKey(userId, yearMonth);
    }

    // monthly 포커스 시간 캐시 키
    private String monthlyFocusTimeKey(Long userId, YearMonth yearMonth) {
        return RedisStatsKeyUtil.monthlyFocusDailyKey(userId, yearMonth);
    }

    // 캐시 존재 여부 키
    private String monthlyFocusTimeExistsKey(Long userId, YearMonth yearMonth) {
        return RedisStatsKeyUtil.monthlyFocusDailyExistsKey(userId, yearMonth);
    }

    // Redis 읽기 예외를 도메인 예외로 변환
    private <T> T executeRead(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (RedisConnectionFailureException e) {
            throw new RedisOperationException(RedisErrorCode.REDIS_CONNECTION_FAILED, e);
        } catch (QueryTimeoutException e) {
            throw new RedisOperationException(RedisErrorCode.REDIS_TIMEOUT, e);
        } catch (SerializationException e) {
            throw new RedisOperationException(RedisErrorCode.REDIS_SERIALIZATION_FAILED, e);
        } catch (DataAccessException e) {
            throw new RedisOperationException(RedisErrorCode.REDIS_OPERATION_FAILED, e);
        }
    }

    // Redis 쓰기 작업을 공통 예외 처리로 실행
    private void executeWrite(Runnable operation) {
        executeRead(() -> {
            operation.run();
            return null;
        });
    }

    private int parseIntegerOrDefault(String value, int defaultValue, String key) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (RuntimeException e) {
            log.warn("정수 캐시 값 형식 오류로 기본값을 사용합니다. key={}, value={}", key, value);
            return defaultValue;
        }
    }
}
