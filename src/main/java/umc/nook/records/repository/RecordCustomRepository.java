package umc.nook.records.repository;

import umc.nook.records.dto.RecordDTO;
import umc.nook.users.domain.User;

import java.time.Year;

public interface RecordCustomRepository {
    RecordDTO.MonthlyRecordRateResponseDTO viewRecordRate(User user, Year year);

}
