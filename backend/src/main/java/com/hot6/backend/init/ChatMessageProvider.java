package com.hot6.backend.init;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

//@Component
public class ChatMessageProvider {

    private final List<String> messages;
    private final Random random = new Random();

    public ChatMessageProvider() {
        this.messages = loadMessages();
    }

    private List<String> loadMessages() {
        try {
            InputStream is = getClass().getResourceAsStream("/static/chat_messages.txt");
            if (is == null) throw new RuntimeException("chat_messages.txt not found in resources/static/");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            throw new RuntimeException("채팅 메시지 로딩 중 오류 발생", e);
        }
    }

    public String getRandomMessage() {
        if (messages.isEmpty()) return "메시지 없음";
        return messages.get(random.nextInt(messages.size()));
    }
}
