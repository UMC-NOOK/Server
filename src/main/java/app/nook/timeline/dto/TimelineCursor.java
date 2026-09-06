package app.nook.timeline.dto;

import java.time.LocalDateTime;

public record TimelineCursor(LocalDateTime occurredAt, Long timelineId) {}
