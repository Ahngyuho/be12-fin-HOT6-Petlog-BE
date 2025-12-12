package com.hot6.backend.init;


import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

//@Component
public class ChatRoomTitleProvider {

    private final List<String> titles;
    private final Random random = new Random();

    public ChatRoomTitleProvider() {
        this.titles = loadTitles();
    }

    private List<String> loadTitles() {
        try {
            InputStream is = getClass().getResourceAsStream("/static/chatroom_titles.txt");
            if (is == null) throw new RuntimeException("chatroom_titles.txt not found in resources/static/");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            throw new RuntimeException("채팅방 제목 로딩 중 오류 발생", e);
        }
    }

    public String getRandomTitle() {
        if (titles.isEmpty()) return "채팅방";
        return titles.get(random.nextInt(titles.size()));
    }
}