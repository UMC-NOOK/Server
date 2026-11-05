package app.nook.user.validation;

import app.nook.user.dto.UserProfileDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class NickNameValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private String violation(String nickName) {
        return validator.validate(new UserProfileDto.NickNameUpdateRequest(nickName)).stream()
                .map(v -> v.getMessage()).findFirst().orElse(null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "한", "1", "홍 길동", "a b c", "Nook123", "가나다라마바사아자차", "가나다라 마바사아자"})
    @DisplayName("1~10자, 영문·숫자·한글·중간 공백은 통과")
    void valid(String nickName) {
        assertThat(violation(nickName)).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "     "})
    @DisplayName("비어있거나 공백만 있으면 모달1 메시지")
    void empty(String nickName) {
        assertThat(violation(nickName)).isEqualTo(NickName.EMPTY_MESSAGE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"가나다라마바사아자차카", "가나다라 마바사아자차"})
    @DisplayName("공백 포함 10자 초과는 길이 메시지")
    void tooLong(String nickName) {
        assertThat(violation(nickName)).isEqualTo(NickName.TOO_LONG_MESSAGE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"nook!", "홍_길동", "😀", "a-b", "ㅋㅋ", " 홍길동", "홍길동 ", " a ", "a\tb", "a\nb"})
    @DisplayName("허용 외 문자 또는 앞뒤 공백은 모달2 메시지")
    void invalidChar(String nickName) {
        assertThat(violation(nickName)).isEqualTo(NickName.INVALID_CHAR_MESSAGE);
    }
}
