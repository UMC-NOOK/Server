package umc.nook.records.gpt;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class GptService {
    @Value("${openai.api.key}")
    private String apikey;

    private final ChatRecordRepository chatRecordRepository;

    private final ObjectMapper objectMapper;

    public JsonNode callChatGpt(Long bookshelfId, String userName, String author, String title, String userMsg) throws JsonProcessingException {
        final String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apikey);

        // system 프롬프트 구성
        String systemContent = String.format(
                """
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
                        - 당신의 응답은 항상 아래 JSON 형식으로 출력합니다:
                                
                        {
                          "isEssay": true or false,
                          "content": "실제 응답 내용"
                        }
                                
                        - 감상문을 출력할 수 있는 조건이 충족되었을 경우, isEssay는 true이고, content는 감상문 본문입니다.
                        - 감상문 조건이 충족되지 않았을 경우, isEssay는 false이고, content는 사용자의 감정을 확장하기 위한 질문입니다.
                        ※ 주의: 반드시 응답 전체를 위 JSON 형식으로만 출력하세요. 그 외의 문장이나 설명은 절대 포함하지 마세요.                    
                        """,
                author, title, userName, userName, userName, userName
        );

        List<Map<String, String>> messages = new ArrayList<>();

        // 2. system role message
        messages.add(Map.of("role", "system", "content", systemContent));

        // 3. 이전 대화 불러오기
        List<ChatRecord> history = chatRecordRepository.findByBookshelfIdOrderByCreatedDate(bookshelfId);
        for (ChatRecord record : history) {
            messages.add(Map.of(
                    "role", record.getRole().toString().toLowerCase(),  // USER / ASSISTANT
                    "content", record.getContent()
            ));
        }

        // 4. 사용자 입력 추가
        messages.add(Map.of("role", "user", "content", userMsg));

        // 5. Request 구성
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("model", "gpt-4-0613");
        bodyMap.put("messages", messages);

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
            String rawContent = responseNode
                    .path("choices").get(0)
                    .path("message")
                    .path("content")
                    .asText();

            // 문자열 자체가 JSON인지 파싱 시도
            ObjectMapper mapper = new ObjectMapper();
            JsonNode parsedNode = mapper.readTree(rawContent);

            // 구조 유효성 검사
            if (!parsedNode.has("isEssay") || !parsedNode.has("content")) {
                throw new CustomException(ErrorCode.GPT_RESPONSE_FORMAT_ERROR);
            }

            // DTO로 변환
            return mapper.treeToValue(parsedNode, GptDTO.ChatRecordDTO.class);

        } catch (JsonProcessingException e) {
            log.error("GPT 응답 파싱 실패", e);
            throw new CustomException(ErrorCode.GPT_RESPONSE_FORMAT_ERROR);
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            log.error("GPT 응답 구조 이상 (choices 배열 또는 message 누락)", e);
            throw new CustomException(ErrorCode.GPT_RESPONSE_FORMAT_ERROR);
        }
    }


}