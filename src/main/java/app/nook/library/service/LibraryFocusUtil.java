package app.nook.library.service;

import app.nook.library.dto.FocusTimeSlot;

public class LibraryFocusUtil {

    // 하루에 포커스 시간 계산, 한달 중 가장 많이 포커스한 날짜 기준으로 나머지를 계산
    public static FocusTimeSlot toFocusTimeSlot(long daySec, long maxDaySec) {
        if (daySec <= 0 || maxDaySec <= 0) {
            return FocusTimeSlot.FOCUS_00;
        }

        double ratio = (double) daySec / (double) maxDaySec;
        if (ratio <= 0.25d) {
            return FocusTimeSlot.FOCUS_01;
        }
        if (ratio <= 0.50d) {
            return FocusTimeSlot.FOCUS_02;
        }
        if (ratio <= 0.75d) {
            return FocusTimeSlot.FOCUS_03;
        }
        return FocusTimeSlot.FOCUS_04;
    }
}
