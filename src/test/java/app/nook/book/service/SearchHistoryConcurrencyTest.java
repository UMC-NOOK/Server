package app.nook.book.service;

import app.nook.book.domain.SearchHistory;
import app.nook.book.domain.enums.SearchType;
import app.nook.book.repository.SearchHistoryRepository;
import app.nook.global.common.AbstractPostgresContainerTests;
import app.nook.global.config.QueryDslConfig;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@DataJpaTest
@ActiveProfiles("test")
@Import({QueryDslConfig.class, SearchHistoryService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SearchHistoryConcurrencyTest extends AbstractPostgresContainerTests {

    private static final String CONCURRENT_KEYWORD = "동시 검색어";

    @MockitoSpyBean
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SearchHistoryService searchHistoryService;

    @Autowired
    private EntityManager entityManager;

    private ExecutorService executor;
    private User user;

    @BeforeEach
    void setUp() {
        searchHistoryRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.saveAndFlush(User.builder()
                .email("concurrency@example.com")
                .nickName("동시성테스터")
                .provider("google")
                .providerId("concurrency-provider")
                .role(UserRole.USER)
                .build());
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "executor가 종료되어야 한다");
        searchHistoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("동일 검색어를 동시에 저장해도 두 요청이 성공하고 한 건만 남는다")
    void 동일검색어_동시저장_한건유지() throws Exception {
        // given
        forceConcurrentSaveAttempt();

        // when
        List<Throwable> failures = runConcurrently(
                () -> searchHistoryService.saveKeyword(user.getId(), CONCURRENT_KEYWORD, SearchType.GLOBAL),
                () -> searchHistoryService.saveKeyword(user.getId(), CONCURRENT_KEYWORD, SearchType.GLOBAL));

        // then
        assertThat(failures).containsOnlyNulls();
        List<SearchHistory> histories = searchHistoryRepository.findAllRecent(user, SearchType.GLOBAL);
        assertThat(histories)
                .singleElement()
                .extracting(SearchHistory::getKeyword)
                .isEqualTo(CONCURRENT_KEYWORD);
    }

    private void forceConcurrentSaveAttempt() {
        CountDownLatch savesReady = new CountDownLatch(2);
        doAnswer(invocation -> {
            savesReady.countDown();
            savesReady.await(1, TimeUnit.SECONDS);
            SearchHistory history = invocation.getArgument(0);
            entityManager.persist(history);
            return history;
        }).when(searchHistoryRepository).save(any(SearchHistory.class));
    }

    private List<Throwable> runConcurrently(ThrowingRunnable first, ThrowingRunnable second)
            throws InterruptedException {
        CountDownLatch workersReady = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<Void> firstFuture = executor.submit(() -> runAfterStart(workersReady, start, first));
        Future<Void> secondFuture = executor.submit(() -> runAfterStart(workersReady, start, second));

        assertTrue(workersReady.await(5, TimeUnit.SECONDS), "두 작업자가 준비되어야 한다");
        start.countDown();

        return Arrays.asList(awaitFailure(firstFuture), awaitFailure(secondFuture));
    }

    private Void runAfterStart(CountDownLatch workersReady, CountDownLatch start, ThrowingRunnable task)
            throws Exception {
        workersReady.countDown();
        assertTrue(start.await(5, TimeUnit.SECONDS), "동시 시작 신호를 받아야 한다");
        task.run();
        return null;
    }

    private Throwable awaitFailure(Future<Void> future) throws InterruptedException {
        try {
            future.get(5, TimeUnit.SECONDS);
            return null;
        } catch (ExecutionException exception) {
            return exception.getCause();
        } catch (Exception exception) {
            return exception;
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
