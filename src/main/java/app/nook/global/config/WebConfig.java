package app.nook.global.config;

import app.nook.book.domain.enums.SearchType;
import app.nook.book.exception.SearchErrorCode;
import app.nook.global.exception.CustomException;
import app.nook.global.response.ErrorCode;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 검색 타입 대소문자 변환
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, SearchType.class, source -> {
            if (source.isBlank()) {
                throw new CustomException(SearchErrorCode.INVALID_SEARCH_TYPE);
            }
            try {
                return SearchType.valueOf(source.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new CustomException(SearchErrorCode.INVALID_SEARCH_TYPE);
            }
        });
    }
}
