package umc.nook.aladin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import umc.nook.aladin.dto.AladinResponseDTO;

@Service
@RequiredArgsConstructor
public class AladinService {

    private final WebClient aladinWebClient;

    @Value("${aladin.ttbkey}")
    private String ttbKey;

    public Mono<AladinResponseDTO.LoungeResultDTO> fetchBooks(String queryType, String searchTarget, int start,
                                                      int maxResults, String categoryId) {
        // type 검증/구분 필요 (book, foreign, ebook)
        return aladinWebClient.get()
                .uri(uriBuilder -> {
                    uriBuilder
                            .path("/ItemList.aspx")
                            .queryParam("ttbkey", ttbKey)
                            .queryParam("QueryType", queryType)
                            .queryParam("MaxResults", maxResults)
                            .queryParam("start", start)
                            .queryParam("SearchTarget", searchTarget)
                            .queryParam("SubSearchTarget", "Book")
                            .queryParam("output", "js")
                            .queryParam("Version", "20131101");
                    if (categoryId != null && !categoryId.isBlank()) {
                        uriBuilder.queryParam("CategoryId", categoryId);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                //.onStatus(status -> ..) 예외처리 필요
                .bodyToMono(AladinResponseDTO.LoungeResultDTO.class);
    }

    public Mono<AladinResponseDTO.SearchResultDTO> searchBooks(String query, int start, int maxResults) {
        return aladinWebClient.get()
                .uri(uriBuilder -> {
                    return uriBuilder
                            .path("/ItemSearch.aspx")
                            .queryParam("ttbkey", ttbKey)
                            .queryParam("Query", query)
                            .queryParam("MaxResults", maxResults)
                            .queryParam("start", start)
                            .queryParam("output", "js")
                            .queryParam("Version", "20131101")
                            .build();
                })
                .retrieve()
                .bodyToMono(AladinResponseDTO.SearchResultDTO.class);
    }

}
