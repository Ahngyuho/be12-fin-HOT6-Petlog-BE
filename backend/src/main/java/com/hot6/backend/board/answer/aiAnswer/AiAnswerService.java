package com.hot6.backend.board.answer.aiAnswer;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

//@Service
@RequiredArgsConstructor
public class AiAnswerService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    public String generateAnswer(String title, String content) {
        System.out.println(">>> [OPENAI KEY] " + apiKey);

        String prompt = String.format("Q: %s\n\n%s\n\n이 질문에 대해 친절하게 답해주세요.", title, content);

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(
                        Map.of("role", "system", "content", "당신은 반려동물 분야 전문 상담 AI입니다."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        try {
            Map<String, Object> response = webClient.post()
                    .uri("/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("choices")) {
                List<?> choices = (List<?>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> message = (Map<?, ?>) choice.get("message");
                    return message.get("content").toString();
                }
            }

        } catch (WebClientResponseException e) {
            System.err.println(">>> GPT 호출 실패: " + e.getStatusCode());
            System.err.println(">>> GPT 응답 바디: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println(">>> GPT 예외 발생: " + e.getMessage());
        }

        return null;
    }
}
