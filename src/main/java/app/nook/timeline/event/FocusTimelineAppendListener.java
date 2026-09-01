package app.nook.timeline.event;

import app.nook.timeline.service.TimelineCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FocusTimelineAppendListener {

    private final TimelineCommandService timelineCommandService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FocusTimelineAppendEvent event) {
        for (Long focusId : event.focusIds()) {
            try {
                timelineCommandService.appendFocusCompleted(focusId);
            } catch (RuntimeException exception) {
                log.warn("[FOCUS_TIMELINE] 타임라인 저장 실패 focusId={}", focusId, exception);
            }
        }
    }
}
