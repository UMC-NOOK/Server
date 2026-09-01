package app.nook.timeline.event;

import java.util.List;

public record FocusTimelineAppendEvent(List<Long> focusIds) {

    public FocusTimelineAppendEvent {
        focusIds = List.copyOf(focusIds);
    }
}
