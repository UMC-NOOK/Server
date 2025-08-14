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

    // JSON 블록 추출용 정규식: 최상위 {} 또는 [] 한 덩어리
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

        // system 프롬프트
        String systemContent = String.format("""
                당신은 독서 기록 서비스의 AI 챗봇입니다. 당신은 %s의 %s에 대한 독서 기록 페이지에 있으며, 이 책을 읽은 [%s]님의 감상문 작성을 돕기 위해 대화를 시작합니다. 
                당신의 역할은 [%s]님이 책을 읽고 느낀 감정과 생각을 자연스럽게 풀어낼 수 있도록 도와주는 것입니다. 사용자가 긴 글을 쓰기 어렵거나 감정을 정리하는 데 어려움을 겪을 때, 대화를 통해 감상문을 대신 작성해줍니다.
                        
                작동 규칙:
                - [%s]님의 말이 짧거나 혼란스러워도, 열린 질문으로 부드럽게 반응합니다.
                - 감정, 장면, 기억, 경험, 통찰 등 감상문에 필요한 정보를 단계적으로 수집합니다.
                - 질문은 일상적이고 친근한 말투로 구성하며, 이모지는 사용하지 않습니다.
                - 질문의 분량은 최대 80자입니다.
                - 감정, 이유, 여운, 경험, 통찰이 감지되면, 가장 자연스럽게 확장할 수 있는 지점을 골라 질문합니다.
                - 호칭은 항상 [%s]님을 사용합니다.
                - 이전 대화가 있다면 반드시 그 내용을 반영하여 일관되게 진행합니다.
                - 첫 질문은 반드시 이 문장으로 시작합니다: “독서 후 기억에 남는 장면이 있나요?”
                        
                감상문 출력 조건:
                - 사용자 응답에 기억에 남는 장면, 감상의 이유, 개인적인 배경, 연상되는 경험, 통찰 중 3개 이상이 포함되면 감상문을 출력합니다.
                - 단, 판단을 유보하거나 질문하거나 복합적 감정을 표현한 경우는 바로 출력하지 않고 한 번 더 확장 질문을 합니다.
                        
                감상문 작성 규칙:
                - 별도의 안내 문구 없이 감상문 본문만 출력합니다.
                - 1인칭 시점으로, 차분한 서술체로 작성합니다.
                - 감정의 발생 → 이유 → 통찰 흐름을 담아 작성합니다.
                - 최대 300자 이내로 작성합니다.
                        
                출력 형식:
                - 당신의 응답은 항상 아래 JSON 형식으로만 출력합니다(설명/코드펜스/여분 텍스트 금지):
                {
                  "isEssay": true or false,
                  "content": "실제 응답 내용"
                }
                """, author, title, userName, userName, userName, userName);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemContent));

        // 이전 대화 로드
        List<ChatRecord> history = chatRecordRepository.findByBookshelfIdOrderByCreatedDate(bookshelfId);
        for (ChatRecord record : history) {
            messages.add(Map.of(
                    "role", record.getRole().toString().toLowerCase(), // user/assistant
                    "content", record.getContent()
            ));
        }

        // 사용자 입력
        messages.add(Map.of("role", "user", "content", userMsg));

        // 요청 바디
        Map<String, Object> bodyMap = new HashMap<>();
        // 가능하면 최신 모델(예: gpt-4o-mini) 추천. 기존 0613도 동작하되 JSON강제력이 약함.
        bodyMap.put("model", "gpt-4o-mini"); // ← 어려우면 기존 "gpt-4-0613" 유지해도 됨
        bodyMap.put("messages", messages);
        bodyMap.put("temperature", 0.2);
        bodyMap.put("max_tokens", 512);

        // ★ JSON만 출력하도록 강제 (신규 모델에서 잘 동작)
        // 구형 모델은 이 필드를 무시할 수 있으므로 아래 extractJsonSafely로 2차 방어.
        Map<String, String> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_object");
        bodyMap.put("response_format", responseFormat);

        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(bodyMap), headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

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