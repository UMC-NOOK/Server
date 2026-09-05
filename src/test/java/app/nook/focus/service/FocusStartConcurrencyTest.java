package app.nook.focus.service;

import app.nook.book.domain.Book;
import app.nook.book.repository.BookRepository;
import app.nook.focus.domain.Focus;
import app.nook.focus.dto.FocusRequestDto;
import app.nook.focus.exception.FocusErrorCode;
import app.nook.focus.repository.FocusRepository;
import app.nook.global.common.AbstractPostgresContainerTests;
import app.nook.global.config.ClockConfig;
import app.nook.global.config.QueryDslConfig;
import app.nook.global.exception.CustomException;
import app.nook.library.domain.Library;
import app.nook.library.repository.LibraryRepository;
import app.nook.user.domain.User;
import app.nook.user.domain.enums.UserRole;
import app.nook.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
@Import({QueryDslConfig.class, ClockConfig.class, FocusService.class, FocusCompletionSegmenter.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FocusStartConcurrencyTest extends AbstractPostgresContainerTests {

    @Autowired
    private FocusService focusService;

    @MockitoSpyBean
    private FocusRepository focusRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private EntityManager entityManager;

    private ExecutorService executor;
    private User user;
    private Book firstBook;
    private Book secondBook;

    @BeforeEach
    void setUp() {
        focusRepository.deleteAll();
        libraryRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.saveAndFlush(User.builder()
                .email("focus-concurrency@example.com")
                .nickName("포커스테스터")
                .provider("google")
                .providerId("focus-concurrency")
                .role(UserRole.USER)
                .build());
        firstBook = bookRepository.saveAndFlush(Book.builder().title("첫 번째 책").author("저자").build());
        secondBook = bookRepository.saveAndFlush(Book.builder().title("두 번째 책").author("저자").build());
        libraryRepository.saveAndFlush(Library.builder().user(user).book(firstBook).build());
        libraryRepository.saveAndFlush(Library.builder().user(user).book(secondBook).build());
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "executor가 종료되어야 한다");
        focusRepository.deleteAll();
        libraryRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
    }

    @ParameterizedTest(name = "동시 시작 시 하나만 성공한다: 같은 책={0}")
    @ValueSource(booleans = {true, false})
    void onlyOneStartSucceeds(boolean sameBook) throws Exception {
        forceConcurrentSaveAttempt();

        List<FocusErrorCode> outcomes = runConcurrently(
                firstBook.getId(), sameBook ? firstBook.getId() : secondBook.getId());

        assertThat(outcomes).containsExactlyInAnyOrder(null, FocusErrorCode.FOCUS_ALREADY_IN_PROGRESS);
        assertThat(focusRepository.findAll()).singleElement()
                .satisfies(focus -> assertThat(focus.getEndedAt()).isNull());
    }

    private void forceConcurrentSaveAttempt() {
        CountDownLatch savesReady = new CountDownLatch(2);
        doAnswer(invocation -> {
            savesReady.countDown();
            savesReady.await(1, TimeUnit.SECONDS);
            Focus focus = invocation.getArgument(0);
            entityManager.persist(focus);
            return focus;
        }).when(focusRepository).save(any(Focus.class));
    }

    private List<FocusErrorCode> runConcurrently(Long firstBookId, Long secondBookId) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<FocusErrorCode> first = executor.submit(() ->
                    startAfterSignal(user, firstBookId, ready, start));
            Future<FocusErrorCode> second = executor.submit(() ->
                    startAfterSignal(user, secondBookId, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS), "두 작업자가 준비되어야 한다");
            start.countDown();

            return Arrays.asList(
                    first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            start.countDown();
        }
    }

    private FocusErrorCode startAfterSignal(User user, Long bookId, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        assertTrue(start.await(5, TimeUnit.SECONDS), "동시 시작 신호를 받아야 한다");
        try {
            focusService.startFocus(user, new FocusRequestDto.FocusStart(bookId));
            return null;
        } catch (CustomException exception) {
            assertThat(exception.getErrorCode()).isEqualTo(FocusErrorCode.FOCUS_ALREADY_IN_PROGRESS);
            return FocusErrorCode.FOCUS_ALREADY_IN_PROGRESS;
        }
    }
}
