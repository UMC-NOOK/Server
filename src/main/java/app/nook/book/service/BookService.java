package app.nook.book.service;

import app.nook.aladin.dto.AladinResponseDto;
import app.nook.aladin.service.AladinService;
import app.nook.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {
    private final AladinService aladinService;
    private final BookRepository bookRepository;

    // 주간 베스트셀러
    public List<AladinResponseDto.BookPreviewDto> getWeeklyBestsellers() {
        return aladinService.getWeeklyBestsellers();
    }

    // 사용자 맞춤 추천 베스트셀러
    // 유저 + 카테고리 추출은 이후 추가 예정
    public List<AladinResponseDto.BookPreviewDto> getPersonalizedBestsellers() {
        String categoryId = "1"; // 예시 카테고리 ID
        return aladinService.getBestSellersByCategory(categoryId);
    }

}
