package umc.nook.records.repository;

public interface RecentRecordProjection {
    Long getBookId();
    String getTitle();
    String getCoverImageUrl();
}