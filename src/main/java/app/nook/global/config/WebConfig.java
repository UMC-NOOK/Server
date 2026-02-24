package app.nook.global.config;

import app.nook.book.domain.enums.SearchType;
import app.nook.book.exception.SearchErrorCode;
import app.nook.global.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads/covers}")
    private String uploadDir;

    // 검색 타입 문자열을 SearchType enum으로 변환
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

    // /uploads/**를 로컬 업로드 디렉터리와 매핑
    // 보안 설정(WebSecurityConfig)에서 /uploads/** 공개 여부를 함께 관리해야 함
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/covers/**")
                .addResourceLocations(absolutePath);
    }
}

