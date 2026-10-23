package app.nook.book.service;

import app.nook.book.dto.BookResponseDto;
import app.nook.global.config.CacheConfig;
import app.nook.yes24.service.Yes24Service;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalizedBestsellerCacheService {

    private static final int RECOMMENDATION_COUNT = 5;

    private final Yes24Service yes24Service;

    // categoryId를 직접 key로 사용해 캐시 조회 전에 DB를 다시 조회하지 않도록 분리
    @Cacheable(
            cacheNames = CacheConfig.PERSONALIZED_BESTSELLERS_CACHE,
            key = "'category:' + #categoryId",
            sync = true
    )
    public List<BookResponseDto.BookPreviewDto> getByCategoryId(String categoryId) {
        return yes24Service.fetchBestsellers(categoryId, RECOMMENDATION_COUNT);
    }
}
