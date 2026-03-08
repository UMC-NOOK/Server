package app.nook.library.perf.dto;

public enum LibraryStatsPerfDataStage {
    STEP1(10_000),
    STEP2(50_000),
    STEP3(100_000),
    STEP4(500_000);

    private final int focusLogCount;

    LibraryStatsPerfDataStage(int focusLogCount) {
        this.focusLogCount = focusLogCount;
    }

    public int focusLogCount() {
        return focusLogCount;
    }
}
