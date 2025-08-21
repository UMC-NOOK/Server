package umc.nook.records.gpt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import umc.nook.common.exception.CustomException;
import umc.nook.common.response.ErrorCode;
import umc.nook.records.domain.ChatRecord;
import umc.nook.records.dto.GptDTO;
import umc.nook.records.repository.ChatRecordRepository;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class GptService {

    @Value("${openai.api.key}")
    private String apikey;

    private final ChatRecordRepository chatRecordRepository;
    private final ObjectMapper objectMapper;

    // JSON 블록 추출용 정규식
    private static final Pattern JSON_PATTERN = Pattern.compile(
            "(?s)(\\{(?:[^{}]|\\{[^{}]*\\})*\\}|\\[(?:[^\\[\\]]|\\[[^\\[\\]]*\\])*\\])"
    );

    private String abbreviate(String s, int max) {
        if (s == null) return "null";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...(truncated)";
    }

    /**
     * LLM이 설명이나 프롤로그를 붙여도 첫 번째 JSON 블록만 안전 추출해서 파싱
     */
    private Optional<JsonNode> extractJsonSafely(String raw, ObjectMapper om) {
        if (raw == null) return Optional.empty();
        String trimmed = raw.trim();

        // 1) 처음부터 JSON이면 바로 시도
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                return Optional.of(om.readTree(trimmed));
            } catch (Exception ignore) {
                // 아래 패턴 시도로 넘어감
            }
        }

        // 2) 본문에서 첫 JSON 블록 추출
        Matcher m = JSON_PATTERN.matcher(trimmed);
        if (m.find()) {
            String json = m.group(1);
            try {
                return Optional.of(om.readTree(json));
            } catch (Exception ignore) {
                // fall through
            }
        }
        return Optional.empty();
    }

    public JsonNode callChatGpt(Long bookshelfId, String userName, String author, String title, String userMsg) throws JsonProcessingException {
        final String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        headers.setBearerAuth(apikey);

        // system 프롬프트 (수정 반영)
        String systemContent = String.format("""
                당신은 독서 기록 서비스의 AI 챗봇입니다. 지금은 %s의 %s 독서 기록 페이지이며, [%s]님의 감상문 작성을 돕습니다. 당신은 사용자가 책을 읽고 느낀 감정과 생각을 풀어낼 수 있도록 80자 이내의 질문을 합니다. 대화는 최소 3턴에서 최대 5턴까지 이어지며, 감정 · 이유 · 경험 · 여운 · 통찰이 드러나면 그 지점을 확장해 다시 묻습니다. 사용자가 답변하면, 그 답변 속에서 ‘기억에 남는 장면’, ‘그때의 감정’, ‘개인적 경험과의 연관’, ‘얻은 통찰’, ‘책을 다 읽고 난 뒤의 여운’ 중 최소 3개 이상이 확인될 때 마지막에 감상문을 작성합니다. 감상문은 사용자의 답변을 바탕으로 직접 쓴 듯한 1인칭 시점으로 만들어지며, 차분한 서술체를 사용하고 과장된 표현은 피합니다. 최종 감상문은 300자 이내로 제한하며, 별도의 안내 문구 없이 본문만 출력합니다.
                출력 형식:
                        - 당신의 질문 또는 감상은 항상 아래 JSON 형식으로만 출력합니다(설명/코드펜스/여분 텍스트 금지) :
                        {
                        "isEssay": true or false,
                        "content": "실제 응답 내용"
                        }
                """, author, title, userName);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemContent));

        // 이전 대화 로드 후 줄글로 합치기
        List<ChatRecord> history = chatRecordRepository.findByBookshelfIdOrderByCreatedDate(bookshelfId);

        log.info("[GPT] bookshelfId={} 이전 대화 {}건 로드됨", bookshelfId, history.size());

        StringBuilder historyText = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            ChatRecord record = history.get(i);
            log.debug("[GPT][History {}] role={}, content={}", i, record.getRole(), abbreviate(record.getContent(), 200));
            historyText.append(record.getRole())
                    .append(": ")
                    .append(record.getContent())
                    .append("\n");
        }

        if (historyText.length() > 0) {
            messages.add(Map.of("role", "user", "content", "이전 대화 기록:\n" + historyText.toString()));
        }

        // 사용자 입력
        messages.add(Map.of("role", "user", "content", userMsg));

        // 요청 바디
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("model", "gpt-4o-mini");
        bodyMap.put("messages", messages);
        bodyMap.put("temperature", 0.2);
        bodyMap.put("max_tokens", 512);

        Map<String, String> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_object");
        bodyMap.put("response_format", responseFormat);

        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(bodyMap), headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        // GPT 실제 응답 로그 추가
        log.info("[GPT][RawResponse] status={}, bodyHead={}",
                response.getStatusCode(),
                abbreviate(response.getBody(), 1000));

        return objectMapper.readTree(response.getBody());
    }

    public GptDTO.ChatRecordDTO getAssistantMsg(
            Long bookshelfId,
            String userName,
            String author,
            String title,
            String userMsg
    ) {
        try {
            JsonNode responseNode = callChatGpt(bookshelfId, userName, author, title, userMsg);

            // chat.completions 표준 응답에서 텍스트 추출
            String rawContent = responseNode
                    .path("choices").get(0)
                    .path("message")
                    .path("content")
                    .asText(null);

            if (rawContent == null) {
                log.error("GPT 응답에서 content가 null입니다. head={}", abbreviate(responseNode.toString(), 600));
                throw new CustomException(ErrorCode.GPT_RESPONSE_FORMAT_ERROR);
            }

            // 1차: 그대로 JSON 파싱 시도
            ObjectMapper safeMapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            Optional<JsonNode> parsedOpt = extractJsonSafely(rawContent, safeMapper);
            if (parsedOpt.isEmpty()) {
                log.error("LLM 응답에서 JSON 블록을 찾지 못함. rawHead={}", abbreviate(rawContent, 300));
                throw new CustomException(ErrorCode.GPT_RESPONSE_FORMAT_ERROR);
            }

            JsonNode parsed = parsedOpt.get();

            // 스키마 최소 검증
            if (!parsed.has("isEssay") || !parsed.has("content")) {
                log.error("필수 필드 누락(isEssay/content). jsonHead={}", abbreviate(parsed.toString(), 300));
                throw new CustomException(ErrorCode.GPT_RESPONSE_FORMAT_ERROR);
            }

            // DTO 변환
            return safeMapper.treeToValue(parsed, GptDTO.ChatRecordDTO.class);

        } catch (JsonProcessingException e) {
            log.error("GPT 응답 파싱 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.GPT_RESPONSE_FORMAT_ERROR);
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            log.error("GPT 응답 구조 이상(choices/message 누락 가능). head={}", e.getMessage(), e);
            throw new CustomException(ErrorCode.GPT_RESPONSE_FORMAT_ERROR);
        }
    }
}