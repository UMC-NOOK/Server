package umc.nook.records.repository;

import umc.nook.bookshelves.dto.BookShelfDTO;
import umc.nook.records.dto.RecordDTO;
import umc.nook.users.domain.User;

import java.time.Year;
import java.util.Optional;

public interface RecordCustomRepository {
    RecordDTO.MonthlyRecordRateResponseDTO viewRecordRate(User user, Year year);

    Optional<BookShelfDTO.RecentRecordDTO> viewRecentRecordedBook(User user);

}
