package app.nook.aladin.service;

import app.nook.aladin.exception.AladinErrorCode;
import app.nook.book.dto.BookResponseDto;
import app.nook.book.exception.BookErrorCode;
import app.nook.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AladinServiceTest {

    private static final String TEST_TTB_KEY = "test-ttb-key";
    private static final String BASE_URL = "https://www.aladin.co.kr/ttb/api";

    private MockRestServiceServer server;
    private AladinService aladinService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        RestClient restClient = builder.build();
        aladinService = new AladinService(restClient);
        ReflectionTestUtils.setField(aladinService, "ttbKey", TEST_TTB_KEY);
    }

    @Test
    @DisplayName("도서 목록 조회 성공 - 유효한 아이템만 필터링 후 targetCount만큼 반환")
    void fetchItemList_필터링후목표개수반환() {
        // given
        server.expect(once(), requestTo(uriForList()))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("ttbkey", TEST_TTB_KEY))
                .andExpect(queryParam("QueryType", "Bestseller"))
                .andExpect(queryParam("SearchTarget", "BOOK"))
                .andExpect(queryParam("MaxResults", "12"))
                .andRespond(withSuccess(itemListResponseJson(), MediaType.APPLICATION_JSON));

        // when
        List<BookResponseDto.BookPreviewDto> result =
                aladinService.fetchItemList("Bestseller", "BOOK", 3, null);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).title()).isEqualTo("채식주의자");
        assertThat(result.get(0).rank()).isEqualTo(1);
        assertThat(result.get(1).title()).isEqualTo("소년이 온다");
        assertThat(result.get(1).rank()).isEqualTo(2);
        assertThat(result.get(2).title()).isEqualTo("작별하지 않는다");
        assertThat(result.get(2).rank()).isEqualTo(3);

        server.verify();
    }

    @Test
    @DisplayName("도서 목록 조회 - 요청 개수는 최대 50으로 제한")
    void fetchItemList_MaxResults상한50적용() {
        // given
        server.expect(once(), requestTo(uriForListWithCategory("1")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("MaxResults", "50"))
                .andExpect(queryParam("CategoryId", "1"))
                .andRespond(withSuccess(emptyItemResponseJson(100L), MediaType.APPLICATION_JSON));

        // when
        List<BookResponseDto.BookPreviewDto> result =
                aladinService.fetchItemList("ItemNewAll", "BOOK", 20, "1");

        // then
        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("도서 목록 조회 실패 - API 오류 시 ALADIN_API_ERROR")
    void fetchItemList_API오류_예외변환() {
        // given
        server.expect(once(), requestTo(uriForList()))
                .andRespond(withServerError());

        // when
        CustomException ex = assertThrows(
                CustomException.class,
                () -> aladinService.fetchItemList("Bestseller", "BOOK", 3, null)
        );

        // then
        assertThat(ex.getErrorCode()).isEqualTo(AladinErrorCode.ALADIN_API_ERROR);
        server.verify();
    }

    @Test
    @DisplayName("검색 성공 - 첫 페이지에서 size만큼 반환하고 nextCursor 설정")
    void searchItems_첫페이지검색성공() {
        // given
        server.expect(once(), requestTo(uriForSearch("한강", 1)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("Query", "한강"))
                .andExpect(queryParam("Start", "1"))
                .andRespond(withSuccess(searchPageResponseJson(), MediaType.APPLICATION_JSON));

        // when
        BookResponseDto.SearchResultDto result = aladinService.searchItems("한강", null, 2);

        // then
        assertThat(result.totalResults()).isEqualTo(100L);
        assertThat(result.books()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(2);
        assertThat(result.books().get(0).getTitle()).isEqualTo("채식주의자");
        assertThat(result.books().get(0).getMallType()).isEqualTo("국내도서");
        server.verify();
    }

    @Test
    @DisplayName("검색 성공 - cursor가 있으면 같은 페이지의 다음 항목부터 조회")
    void searchItems_커서시작_같은페이지이어받기() {
        // given
        server.expect(once(), requestTo(uriForSearch("한강", 1)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("Query", "한강"))
                .andExpect(queryParam("Start", "1"))
                .andRespond(withSuccess(searchPageForCursorResponseJson(), MediaType.APPLICATION_JSON));

        // when
        BookResponseDto.SearchResultDto result = aladinService.searchItems("한강", 1, 2);

        // then
        assertThat(result.totalResults()).isEqualTo(100L);
        assertThat(result.books()).hasSize(2);
        assertThat(result.books().get(0).getTitle()).isEqualTo("소년이 온다");
        assertThat(result.books().get(1).getTitle()).isEqualTo("작별하지 않는다");
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(3);
        server.verify();
    }

    @Test
    @DisplayName("검색 성공 - 무효 아이템을 건너뛰고 다음 페이지까지 이어서 수집")
    void searchItems_필터링으로다음페이지이월() {
        // given
        server.expect(once(), requestTo(uriForSearch("소설", 1)))
                .andRespond(withSuccess(searchPage1MixedResponseJson(), MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo(uriForSearch("소설", 2)))
                .andRespond(withSuccess(searchPage2ResponseJson(), MediaType.APPLICATION_JSON));

        // when
        BookResponseDto.SearchResultDto result = aladinService.searchItems("소설", null, 3);

        // then
        assertThat(result.books()).hasSize(3);
        assertThat(result.books().get(0).getTitle()).isEqualTo("유효한 책 1");
        assertThat(result.books().get(1).getTitle()).isEqualTo("유효한 책 2");
        assertThat(result.books().get(2).getTitle()).isEqualTo("유효한 책 3");
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(52);
        server.verify();
    }

    @Test
    @DisplayName("검색 종료 - 응답 item이 비어 있으면 다음 페이지 없음")
    void searchItems_빈응답종료() {
        // given
        server.expect(once(), requestTo(uriForSearch("없는책", 1)))
                .andRespond(withSuccess(emptyItemResponseJson(0L), MediaType.APPLICATION_JSON));

        // when
        BookResponseDto.SearchResultDto result = aladinService.searchItems("없는책", null, 10);

        // then
        assertThat(result.totalResults()).isEqualTo(0L);
        assertThat(result.books()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        server.verify();
    }

    @Test
    @DisplayName("상세 조회 성공 - 카테고리 alias를 DB 카테고리명으로 변환")
    void lookupItem_정상매핑() {
        // given
        server.expect(once(), requestTo(uriForLookup("9788936434267")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("ItemId", "9788936434267"))
                .andExpect(queryParam("ItemIdType", "ISBN13"))
                .andRespond(withSuccess(lookupResponseJson(), MediaType.APPLICATION_JSON));

        // when
        BookResponseDto.BookDetailDto result = aladinService.lookupItem("9788936434267");

        // then
        assertThat(result.getIsbn13()).isEqualTo("9788936434267");
        assertThat(result.getTitle()).isEqualTo("채식주의자");
        assertThat(result.getMallTypeCode().name()).isEqualTo("BOOK");
        assertThat(result.getMallType()).isEqualTo("국내도서");
        assertThat(result.getCategory()).isEqualTo("소설/시/희곡");
        assertThat(result.getPages()).isEqualTo(184);
        server.verify();
    }

    @Test
    @DisplayName("상세 조회 실패 - API 오류 시 ALADIN_API_ERROR")
    void lookupItem_API오류_예외변환() {
        // given
        server.expect(once(), requestTo(uriForLookup("9780000000002")))
                .andRespond(withServerError());

        // when
        CustomException ex = assertThrows(
                CustomException.class,
                () -> aladinService.lookupItem("9780000000002")
        );

        // then
        assertThat(ex.getErrorCode()).isEqualTo(AladinErrorCode.ALADIN_API_ERROR);
        server.verify();
    }

    @Test
    @DisplayName("상세 조회 실패 - item이 비어 있으면 ISBN13_NOT_FOUND")
    void lookupItem_item없음_예외() {
        // given
        server.expect(once(), requestTo(uriForLookup("9780000000000")))
                .andRespond(withSuccess(emptyItemResponseJson(0L), MediaType.APPLICATION_JSON));

        // when
        CustomException ex = assertThrows(
                CustomException.class,
                () -> aladinService.lookupItem("9780000000000")
        );

        // then
        assertThat(ex.getErrorCode()).isEqualTo(BookErrorCode.ISBN13_NOT_FOUND);
        server.verify();
    }

    @Test
    @DisplayName("상세 조회 실패 - 정책상 허용되지 않는 도서는 BOOK_NOT_ALLOWED")
    void lookupItem_무효도서_예외() {
        // given
        server.expect(once(), requestTo(uriForLookup("9780000000001")))
                .andRespond(withSuccess(invalidLookupResponseJson(), MediaType.APPLICATION_JSON));

        // when
        CustomException ex = assertThrows(
                CustomException.class,
                () -> aladinService.lookupItem("9780000000001")
        );

        // then
        assertThat(ex.getErrorCode()).isEqualTo(BookErrorCode.BOOK_NOT_ALLOWED);
        server.verify();
    }

    private URI uriForList() {
        return UriComponentsBuilder.fromUriString(BASE_URL)
                .path("/ItemList.aspx")
                .queryParam("ttbkey", TEST_TTB_KEY)
                .queryParam("cover", "Big")
                .queryParam("output", "js")
                .queryParam("Version", "20131101")
                .queryParam("QueryType", "Bestseller")
                .queryParam("SearchTarget", "BOOK")
                .queryParam("MaxResults", 12)
                .build()
                .toUri();
    }

    private URI uriForListWithCategory(String categoryId) {
        return UriComponentsBuilder.fromUriString(BASE_URL)
                .path("/ItemList.aspx")
                .queryParam("ttbkey", TEST_TTB_KEY)
                .queryParam("cover", "Big")
                .queryParam("output", "js")
                .queryParam("Version", "20131101")
                .queryParam("QueryType", "ItemNewAll")
                .queryParam("SearchTarget", "BOOK")
                .queryParam("MaxResults", 50)
                .queryParam("CategoryId", categoryId)
                .build()
                .toUri();
    }

    private URI uriForSearch(String keyword, int start) {
        return UriComponentsBuilder.fromUriString(BASE_URL)
                .path("/ItemSearch.aspx")
                .queryParam("ttbkey", TEST_TTB_KEY)
                .queryParam("cover", "Big")
                .queryParam("output", "js")
                .queryParam("Version", "20131101")
                .queryParam("Query", keyword)
                .queryParam("SearchTarget", "All")
                .queryParam("MaxResults", 50)
                .queryParam("Start", start)
                .build()
                .toUri();
    }

    private URI uriForLookup(String isbn13) {
        return UriComponentsBuilder.fromUriString(BASE_URL)
                .path("/ItemLookUp.aspx")
                .queryParam("ttbkey", TEST_TTB_KEY)
                .queryParam("cover", "Big")
                .queryParam("output", "js")
                .queryParam("Version", "20131101")
                .queryParam("ItemId", isbn13)
                .queryParam("ItemIdType", "ISBN13")
                .build()
                .toUri();
    }

    private String itemListResponseJson() {
        return """
                  {
                    "totalResults": 10,
                    "item": [
                      {
                        "isbn13": "9788936434267",
                        "title": "채식주의자",
                        "author": "한강",
                        "categoryName": "국내도서>소설/시/희곡",
                        "mallType": "BOOK",
                        "publisher": "창비",
                        "cover": "https://img.test/1.jpg",
                        "link": "https://aladin.test/1",
                        "adult": false
                      },
                      {
                        "isbn13": "9780000000001",
                        "title": "성인도서",
                        "author": "작가",
                        "categoryName": "국내도서>소설/시/희곡",
                        "mallType": "BOOK",
                        "publisher": "출판사",
                        "cover": "https://img.test/x.jpg",
                        "link": "https://aladin.test/x",
                        "adult": true
                      },
                      {
                        "isbn13": "9788936433598",
                        "title": "소년이 온다",
                        "author": "한강",
                        "categoryName": "국내도서>소설/시/희곡",
                        "mallType": "BOOK",
                        "publisher": "창비",
                        "cover": "https://img.test/2.jpg",
                        "link": "https://aladin.test/2",
                        "adult": false
                      },
                      {
                        "isbn13": "9788936434120",
                        "title": "작별하지 않는다",
                        "author": "한강",
                        "categoryName": "국내도서>소설/시/희곡",
                        "mallType": "BOOK",
                        "publisher": "문학동네",
                        "cover": "https://img.test/3.jpg",
                        "link": "https://aladin.test/3",
                        "adult": false
                      }
                    ]
                  }
                  """;
    }

    private String searchPageResponseJson() {
        return """
                  {
                    "totalResults": 100,
                    "item": [
                      {
                        "isbn13": "9788936434267",
                        "title": "채식주의자",
                        "author": "한강",
                        "categoryName": "국내도서>소설/시/희곡",
                        "mallType": "BOOK",
                        "publisher": "창비",
                        "pubDate": "2024-01-01",
                        "cover": "https://img.test/1.jpg",
                        "adult": false
                      },
                      {
                        "isbn13": "9788936433598",
                        "title": "소년이 온다",
                        "author": "한강",
                        "categoryName": "국내도서>소설/시/희곡",
                        "mallType": "BOOK",
                        "publisher": "창비",
                        "pubDate": "2024-01-02",
                        "cover": "https://img.test/2.jpg",
                        "adult": false
                      }
                    ]
                  }
                  """;
    }

    private String searchPage1MixedResponseJson() {
        StringBuilder items = new StringBuilder("""
                  {
                    "isbn13": "9781111111111",
                    "title": "유효한 책 1",
                    "author": "작가1",
                    "categoryName": "국내도서>소설/시/희곡",
                    "mallType": "BOOK",
                    "publisher": "출판사1",
                    "pubDate": "2024-01-01",
                    "cover": "https://img.test/a.jpg",
                    "adult": false
                  }
                """);

        for (int i = 1; i <= 49; i++) {
            items.append(",")
                    .append("""
                  {
                    "isbn13": "97822222222%02d",
                    "title": "무효한 책 %d",
                    "author": "작가%d",
                    "categoryName": "국내도서>중고샵",
                    "mallType": "BOOK",
                    "publisher": "출판사%d",
                    "pubDate": "2024-01-%02d",
                    "cover": "https://img.test/b%d.jpg",
                    "adult": false
                  }
                """.formatted(i, i, i + 1, i + 1, (i % 28) + 1, i));
        }

        return """
                  {
                    "totalResults": 200,
                    "item": [
                  %s
                    ]
                  }
                  """.formatted(items);
    }

    private String searchPageForCursorResponseJson() {
        return """
                  {
                    "totalResults": 100,
                    "item": [
                      {
                        "isbn13": "9788936434267",
                        "title": "채식주의자",
                        "author": "한강",
                        "categoryName": "국내도서>소설/시/희곡",
                        "mallType": "BOOK",
                        "publisher": "창비",
                        "pubDate": "2024-01-01",
                        "cover": "https://img.test/1.jpg",
                        "adult": false
                      },
                      {
                        "isbn13": "9788936433598",
                        "title": "소년이 온다",
                        "author": "한강",
                        "categoryName": "국내도서>소설/시/희곡",
                        "mallType": "BOOK",
                        "publisher": "창비",
                        "pubDate": "2024-01-02",
                        "cover": "https://img.test/2.jpg",
                        "adult": false
                      },
                      {
                        "isbn13": "9788936434120",
                        "title": "작별하지 않는다",
                        "author": "한강",
                        "categoryName": "국내도서>소설/시/희곡",
                        "mallType": "BOOK",
                        "publisher": "문학동네",
                        "pubDate": "2024-01-03",
                        "cover": "https://img.test/3.jpg",
                        "adult": false
                      }
                    ]
                  }
                  """;
    }

    private String searchPage2ResponseJson() {
        return """
                  {
                    "totalResults": 200,
                    "item": [
                      {
                        "isbn13": "9783333333333",
                        "title": "유효한 책 2",
                        "author": "작가3",
                        "categoryName": "국내도서>소설/시/희곡",
                        "mallType": "BOOK",
                        "publisher": "출판사3",
                        "pubDate": "2024-01-03",
                        "cover": "https://img.test/c.jpg",
                        "adult": false
                      },
                      {
                        "isbn13": "9784444444444",
                        "title": "유효한 책 3",
                        "author": "작가4",
                        "categoryName": "국내도서>소설/시/희곡",
                        "mallType": "BOOK",
                        "publisher": "출판사4",
                        "pubDate": "2024-01-04",
                        "cover": "https://img.test/d.jpg",
                        "adult": false
                      }
                    ]
                  }
                  """;
    }

    private String lookupResponseJson() {
        return """
                  {
                    "item": [
                      {
                        "isbn13": "9788936434267",
                        "title": "채식주의자",
                        "author": "한강",
                        "categoryName": "국내도서>라이트 노벨",
                        "mallType": "BOOK",
                        "publisher": "창비",
                        "pubDate": "2024-01-01",
                        "description": "설명",
                        "cover": "https://img.test/detail.jpg",
                        "link": "https://aladin.test/detail",
                        "adult": false,
                        "subInfo": {
                          "itemPage": 184
                        }
                      }
                    ]
                  }
                  """;
    }

    private String invalidLookupResponseJson() {
        return """
                  {
                    "item": [
                      {
                        "isbn13": "9780000000001",
                        "title": "허용되지 않는 책",
                        "author": "작가",
                        "categoryName": "국내도서>중고샵",
                        "mallType": "BOOK",
                        "publisher": "출판사",
                        "adult": false
                      }
                    ]
                  }
                  """;
    }

    private String emptyItemResponseJson(Long totalResults) {
        return """
        {
            "totalResults": %d,
                "item": []
        }
        """.formatted(totalResults);
    }
}
