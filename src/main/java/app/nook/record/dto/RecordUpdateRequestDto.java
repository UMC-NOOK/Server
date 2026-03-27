package app.nook.record.dto;

import app.nook.record.domain.enums.Emotion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Length;

import java.util.List;

public record RecordUpdateRequestDto(
        @NotBlank(message = "내용은 필수입니다.")
        @Length(max = 1000, message = "내용은 최대 1000자까지 입력할 수 있습니다.") // TODO : 추후 요구사항 반영해서 수정
        String content,
        Emotion emotion,
        @Size(max = 5, message = "이미지는 최대 5개까지 업로드할 수 있습니다.")
        List<String> imageKeys
) {
}
