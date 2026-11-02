package app.nook.yes24.service;

import app.nook.book.domain.enums.MallType;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.exception.BookErrorCode;
import app.nook.global.exception.CustomException;
import app.nook.yes24.dto.Yes24ResponseDto;
import app.nook.yes24.exception.Yes24ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class Yes24ServiceTest {

    private static final String BASE_URL = "https://apis.yes24.com";
    private static final String API_KEY = "test-yes24-api-key";
    private static final String NOT_FOUND_BODY = """
            {"success":false,"message":"검색 결과가 없습니다.","errorCode":"SEARCH_001","data":null}
            """;

    private MockRestServiceServer server;
    private Yes24Service yes24Service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        yes24Service = new Yes24Service(builder
                .baseUrl(BASE_URL)
                .defaultHeader("X-Api-Key", API_KEY)
                .build());
    }

    @Test
    @DisplayName("검색 원본 페이지 조회 - 국내 종이책·기본 정렬·100개 단위로 요청하고 원본 항목을 그대로 반환")
    void fetchSearchPage_success() throws IOException {
        server.expect(requestTo(startsWith(BASE_URL + "/v1/goods/itemList")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Api-Key", API_KEY))
                .andExpect(queryParam("category", "BOOK"))
                .andExpect(queryParam("sort", "DEFAULT"))
                .andExpect(queryParam("page", "2"))
                .andExpect(queryParam("pageSize", "100"))
                .andRespond(withSuccess(fixture("search-page.json"), MediaType.APPLICATION_JSON));

        Yes24ResponseDto.SearchPage page = yes24Service.fetchSearchPage("한강", 2);

        assertThat(page.totalCount()).isEqualTo(1207L);
        assertThat(page.items()).hasSize(5);
        assertThat(page.items().get(0).getIsbn13()).isEqualTo("9788957946220");
        server.verify();
    }

    @Test
    @DisplayName("검색 원본 페이지는 Redis 캐시 직렬화 후에도 같은 타입으로 복원된다")
    void searchPage_redisSerializationRoundTrip() throws IOException {
        server.expect(requestTo(startsWith(BASE_URL + "/v1/goods/itemList")))
                .andRespond(withSuccess(fixture("search-page.json"), MediaType.APPLICATION_JSON));
        Yes24ResponseDto.SearchPage page = yes24Service.fetchSearchPage("한강", 1);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        Object restored = serializer.deserialize(serializer.serialize(page));

        assertThat(restored).isInstanceOf(Yes24ResponseDto.SearchPage.class);
        Yes24ResponseDto.SearchPage restoredPage = (Yes24ResponseDto.SearchPage) restored;
        assertThat(restoredPage.totalCount()).isEqualTo(1207L);
        assertThat(restoredPage.items()).hasSize(5);
        assertThat(restoredPage.items().get(0)).isInstanceOf(Yes24ResponseDto.Yes24Item.class);
        assertThat(restoredPage.items().get(0).getGoodsSortNm()).isEqualTo("국내도서-소설/시/희곡");
    }

    @Test
    @DisplayName("검색어의 + 같은 예약 문자는 인코딩해서 보낸다")
    void fetchSearchPage_encodesReservedCharacters() throws IOException {
        server.expect(requestTo(startsWith(BASE_URL + "/v1/goods/itemList")))
                .andExpect(queryParam("query", "C%2B%2B"))
                .andRespond(withSuccess(fixture("search-page.json"), MediaType.APPLICATION_JSON));

        yes24Service.fetchSearchPage("C++", 1);

        server.verify();
    }

    @Test
    @DisplayName("YES24가 거부하는 <, >, -- 는 공백으로 바꿔 검색한다")
    void fetchSearchPage_replacesRejectedCharacters() throws IOException {
        server.expect(requestTo(startsWith(BASE_URL + "/v1/goods/itemList")))
                .andExpect(queryParam("query", "%EC%96%B4%EB%A6%B0%EC%99%95%EC%9E%90%20OR%201%3D1"))
                .andRespond(withSuccess(fixture("search-page.json"), MediaType.APPLICATION_JSON));

        yes24Service.fetchSearchPage("<어린왕자> OR 1=1 --", 1);

        server.verify();
    }

    @Test
    @DisplayName("치환 후 검색어가 비면 YES24를 호출하지 않고 빈 페이지")
    void fetchSearchPage_blankAfterReplaceIsEmpty() {
        Yes24ResponseDto.SearchPage page = yes24Service.fetchSearchPage("<> --", 1);

        assertThat(page.items()).isEmpty();
        server.verify(); // 요청이 없어야 함
    }

    @Test
    @DisplayName("YES24가 검색어를 거부(400 PARAM_007)하면 빈 페이지")
    void fetchSearchPage_rejectedQueryIsEmpty() {
        server.expect(requestTo(startsWith(BASE_URL + "/v1/goods/itemList")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"success\":false,\"errorCode\":\"PARAM_007\",\"data\":null}"));

        Yes24ResponseDto.SearchPage page = yes24Service.fetchSearchPage("' OR 1=1", 1);

        assertThat(page.items()).isEmpty();
    }

    @Test
    @DisplayName("그 밖의 400은 BOOK_API_ERROR")
    void execute_otherBadRequest() {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"success\":false,\"errorCode\":\"PARAM_006\",\"data\":null}"));

        assertThatThrownBy(() -> yes24Service.fetchSearchPage("한강", 1))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(Yes24ErrorCode.BOOK_API_ERROR);
    }

    @Test
    @DisplayName("검색 결과 없음(404 SEARCH_001)은 오류가 아니라 빈 페이지")
    void fetchSearchPage_notFoundIsEmpty() {
        server.expect(requestTo(startsWith(BASE_URL + "/v1/goods/itemList")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(NOT_FOUND_BODY));

        Yes24ResponseDto.SearchPage page = yes24Service.fetchSearchPage("없는검색어", 1);

        assertThat(page.items()).isEmpty();
        assertThat(page.totalCount()).isZero();
    }

    @Test
    @DisplayName("상세 조회 - 카테고리 매핑, 출간일 변환, 소개글 HTML 제거, 구매 링크")
    void lookupItem_success() throws IOException {
        server.expect(requestTo(startsWith(BASE_URL + "/v1/goods/itemDetail")))
                .andExpect(queryParam("searchType", "ISBN13"))
                .andExpect(queryParam("query", "9788937427282"))
                .andExpect(queryParam("detail", "Y"))
                .andRespond(withSuccess(fixture("item-detail.json"), MediaType.APPLICATION_JSON));

        BookResponseDto.BookDetailDto result = yes24Service.lookupItem("9788937427282");

        assertThat(result.getTitle()).isEqualTo("내 어머니의 자서전");
        assertThat(result.getCategory()).isEqualTo("소설/시/희곡");
        assertThat(result.getMallTypeCode()).isEqualTo(MallType.BOOK);
        assertThat(result.getPublicationDate()).isEqualTo("2022-11-18");
        assertThat(result.getPages()).isEqualTo(248);
        assertThat(result.getDescription())
                .isEqualTo("꾸준히 노벨 문학상 후보로 거론되는 작가의 대표작.\n민음사에서 출간되었다.<내 어머니의 자서전>");
        assertThat(result.getPurchaseLink()).isEqualTo("https://www.yes24.com/product/goods/115457342");
    }

    @Test
    @DisplayName("상세 조회 - 없는 ISBN(404 GOODS_002)은 ISBN13_NOT_FOUND")
    void lookupItem_notFound() {
        server.expect(requestTo(startsWith(BASE_URL + "/v1/goods/itemDetail")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"success\":false,\"errorCode\":\"GOODS_002\"}"));

        assertThatThrownBy(() -> yes24Service.lookupItem("9780000000000"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(BookErrorCode.ISBN13_NOT_FOUND);
    }

    @Test
    @DisplayName("상세 조회 - 성인 도서는 BOOK_NOT_ALLOWED")
    void lookupItem_adultNotAllowed() throws IOException {
        String adultDetail = fixture("item-detail.json").replaceAll("\"adultYn\":\\s*\"N\"", "\"adultYn\":\"Y\"");
        server.expect(requestTo(startsWith(BASE_URL + "/v1/goods/itemDetail")))
                .andRespond(withSuccess(adultDetail, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> yes24Service.lookupItem("9788937427282"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(BookErrorCode.BOOK_NOT_ALLOWED);
    }

    @Test
    @DisplayName("베스트셀러 - 4배수로 요청하고 유효한 도서만 순위를 다시 매겨 targetCount개 반환")
    void fetchBestsellers_success() throws IOException {
        server.expect(requestTo(startsWith(BASE_URL + "/v1/category/bestseller")))
                .andExpect(queryParam("categoryId", "001001046013"))
                .andExpect(queryParam("pageSize", "20"))
                .andRespond(withSuccess(fixture("bestseller.json"), MediaType.APPLICATION_JSON));

        List<BookResponseDto.BookPreviewDto> result = yes24Service.fetchBestsellers("001001046013", 5);

        assertThat(result).hasSize(5);
        assertThat(result).extracting(BookResponseDto.BookPreviewDto::rank).containsExactly(1, 2, 3, 4, 5);
        assertThat(result.get(0).title()).isEqualTo("오디세이아");
    }

    @Test
    @DisplayName("호출 한도 초과(429)는 BOOK_API_RATE_LIMITED")
    void execute_rateLimited() {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> yes24Service.fetchSearchPage("한강", 1))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(Yes24ErrorCode.BOOK_API_RATE_LIMITED);
    }

    @Test
    @DisplayName("인증 실패(401)는 BOOK_API_UNAUTHORIZED")
    void execute_unauthorized() {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> yes24Service.fetchBestsellers("001001046", 5))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(Yes24ErrorCode.BOOK_API_UNAUTHORIZED);
    }

    @Test
    @DisplayName("서버 오류(5xx)는 BOOK_API_ERROR")
    void execute_serverError() {
        server.expect(requestTo(startsWith(BASE_URL)))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> yes24Service.lookupItem("9788937427282"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(Yes24ErrorCode.BOOK_API_ERROR);
    }

    private String fixture(String name) throws IOException {
        return new ClassPathResource("yes24/" + name).getContentAsString(StandardCharsets.UTF_8);
    }
}
