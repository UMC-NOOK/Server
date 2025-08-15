package umc.nook.aladin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import umc.nook.aladin.converter.AladinConverter;
import umc.nook.aladin.dto.AladinResponseDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AladinService {

    private final RestTemplate restTemplate;

    @Value("${aladin.ttbkey}")
    private String ttbKey;

    private static final String ALADIN_BASE_URL = "https://www.aladin.co.kr/ttb/api";

    public AladinResponseDTO.ResultDTO fetchBooks(
            String queryType, String searchTarget, int start, int maxResults, String categoryId) {

        StringBuilder url = new StringBuilder(ALADIN_BASE_URL + "/ItemList.aspx?");
        url.append("ttbkey=").append(ttbKey)
                .append("&QueryType=").append(queryType)
                .append("&MaxResults=").append(maxResults)
                .append("&start=").append(start)
                .append("&SubSearchTarget=Book")
                .append("&cover=Big")
                .append("&output=js")
                .append("&Version=20131101");
        if (searchTarget != null) url.append("&SearchTarget=").append(searchTarget);
        if (categoryId != null && !categoryId.isBlank()) url.append("&CategoryId=").append(categoryId);

        return restTemplate.getForObject(url.toString(), AladinResponseDTO.ResultDTO.class);
    }

    @Async("taskExecutor")
    public CompletableFuture<AladinResponseDTO.ResultDTO> fetchBooksAsync(
            String queryType, String mallType, int start, int limit, String categoryId) {
        try {
            AladinResponseDTO.ResultDTO result = fetchBooks(queryType, mallType, start, limit, categoryId);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public AladinResponseDTO.ResultDTO searchBooks(String query, int start, int maxResults) {
        StringBuilder url = new StringBuilder(ALADIN_BASE_URL + "/ItemSearch.aspx?");
        url.append("ttbkey=").append(ttbKey)
                .append("&Query=").append(query)
                .append("&SearchTarget=").append("book")
                .append("&Sort=").append("SalesPoint")
                .append("&MaxResults=").append(maxResults)
                .append("&start=").append(start)
                .append("&cover=Big")
                .append("&output=js")
                .append("&Version=20131101");
        return restTemplate.getForObject(url.toString(), AladinResponseDTO.ResultDTO.class);
    }

    public AladinResponseDTO.ResultDTO lookUpBook(String isbn13) {
        StringBuilder url = new StringBuilder(ALADIN_BASE_URL + "/ItemLookUp.aspx?");
        url.append("ttbkey=").append(ttbKey)
                .append("&itemIdType=ISBN13")
                .append("&itemId=").append(isbn13)
                .append("&cover=Big")
                .append("&output=js")
                .append("&Version=20131101");
        return restTemplate.getForObject(url.toString(), AladinResponseDTO.ResultDTO.class);
    }

    public AladinResponseDTO.BestInThisCategoryDTO getBestCategoryBooks(String categoryId) {
        StringBuilder url = new StringBuilder(ALADIN_BASE_URL + "/ItemList.aspx?");
        url.append("ttbkey=").append(ttbKey)
                .append("&QueryType=Bestseller")
                .append("&MaxResults=5")
                .append("&SubSearchTarget=Book")
                .append("&cover=Big")
                .append("&output=js")
                .append("&Version=20131101")
                .append("&CategoryId=").append(categoryId);
        return restTemplate.getForObject(url.toString(), AladinResponseDTO.BestInThisCategoryDTO.class);
    }

//    public AladinResponseDTO.ResultDTO searchBooksByBestResult(String query, int start, int maxResults) {
//        List<String> targets = List.of("Book", "Foreign", "Ebook");
//        AladinResponseDTO.ResultDTO bestResult = null;
//
//        for (String target : targets) {
//            AladinResponseDTO.ResultDTO result = searchBooks(query, start, maxResults, target);
//
//            if (result == null) continue;
//
//            if (bestResult == null || result.getTotalResults() > bestResult.getTotalResults()) {
//                bestResult = result;
//            }
//        }
//
//        return bestResult;
//    }
}
