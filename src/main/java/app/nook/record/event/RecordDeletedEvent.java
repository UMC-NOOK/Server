package app.nook.record.event;

import java.util.List;

public record RecordDeletedEvent(
        Long recordId,
        List<String> imageKeys
) {
}
