package app.nook.book.dto;

import java.util.List;

public class LibrarySearchHomeResponseDto {

    private LibrarySearchHomeResponseDto() {
    }

    public record Result(
            List<Section> sections
    ) {}

    public sealed interface Section
            permits RecentFocusSection, BeforeReadingSection, RecommendationSection {

        String type();

        String title();
    }

    public record RecentFocusSection(
            String type,
            String title,
            List<RecentFocusItem> items
    ) implements Section {

        public static RecentFocusSection of(List<RecentFocusItem> items) {
            return new RecentFocusSection("RECENT_FOCUS", "최근 포커스한 책", items);
        }
    }

    public record BeforeReadingSection(
            String type,
            String title,
            List<BeforeReadingItem> items
    ) implements Section {

        public static BeforeReadingSection of(List<BeforeReadingItem> items) {
            return new BeforeReadingSection("BEFORE_READING", "아직 읽지 않은 책", items);
        }
    }

    public record RecommendationSection(
            String type,
            String title,
            List<RecommendationItem> items
    ) implements Section {

        public static RecommendationSection of(List<RecommendationItem> items) {
            return new RecommendationSection("RECOMMENDATION", "이 책을 추천해요", items);
        }
    }

    public record RecentFocusItem(
            Long bookId,
            String title,
            String author,
            String coverUrl
    ) {}

    public record BeforeReadingItem(
            Long bookId,
            String title,
            String author,
            String coverUrl
    ) {}

    public record RecommendationItem(
            String isbn13,
            String title,
            String author,
            String coverUrl
    ) {}
}
