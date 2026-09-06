package app.nook.timeline.event;

import app.nook.timeline.service.TimelineCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FocusTimelineAppendListenerTest {

    @Mock
    private TimelineCommandService timelineCommandService;

    @InjectMocks
    private FocusTimelineAppendListener listener;

    @Test
    void timelineFailureDoesNotEscapeAndRemainingFocusesAreProcessed() {
        doThrow(new IllegalStateException("timeline failure"))
                .when(timelineCommandService).appendFocusCompleted(100L);

        assertThatCode(() -> listener.handle(new FocusTimelineAppendEvent(List.of(100L, 101L))))
                .doesNotThrowAnyException();

        verify(timelineCommandService).appendFocusCompleted(100L);
        verify(timelineCommandService).appendFocusCompleted(101L);
    }
}
