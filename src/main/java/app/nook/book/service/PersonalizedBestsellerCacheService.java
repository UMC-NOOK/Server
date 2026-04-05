package app.nook.book.service;

import app.nook.aladin.service.AladinService;
import app.nook.book.dto.BookResponseDto;
import app.nook.global.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalizedBestsellerCacheService {

    private final AladinService aladinService;

    // categoryId를 직접 key로 사용해 캐시 조회 전에 DB를 다시 조회하지 않도록 분리
    @Cacheable(
            cacheNames = CacheConfig.PERSONALIZED_BESTSELLERS_CACHE,
            key = "'category:' + #categoryId",
            sync = true
    )
    public List<BookResponseDto.BookPreviewDto> getByCategoryId(int categoryId) {
        return aladinService.fetchItemList(
                "Bestseller",
                "BOOK",
                5,
                String.valueOf(categoryId)
        );
    }
}
