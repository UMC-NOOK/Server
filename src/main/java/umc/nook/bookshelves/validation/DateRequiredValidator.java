package umc.nook.bookshelves.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import umc.nook.bookshelves.domain.ReadingStatus;
import umc.nook.bookshelves.dto.BookShelfDTO;

public class DateRequiredValidator implements ConstraintValidator<DateRequiredIfReadingOrFinished, BookShelfDTO.RegisterBookDTO> {

    @Override
    public boolean isValid(BookShelfDTO.RegisterBookDTO dto, ConstraintValidatorContext context) {
        if (dto == null) return true;

        if (dto.getReadingStatus() == ReadingStatus.READING
                || dto.getReadingStatus() == ReadingStatus.FINISHED) {
            return dto.getDate() != null;
        }
        return true;
    }
}
