package com.hot6.backend.init;


import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

//@Component
public class ChatRoomHashtagProvider {

    private final List<String> hashtags;
    private final Random random = new Random();

    public ChatRoomHashtagProvider() {
        this.hashtags = loadHashtags();
    }

    private List<String> loadHashtags() {
        try {
            InputStream is = getClass().getResourceAsStream("/static/chatroom_hashtags.txt");
            if (is == null) throw new RuntimeException("chatroom_hashtags.txt not found in resources/static/");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .map(tag -> tag.startsWith("#") ? tag : "#" + tag) // 해시태그 형식 보장
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            throw new RuntimeException("채팅방 해시태그 로딩 중 오류 발생", e);
        }
    }

    public String getRandomHashtag() {
        if (hashtags.isEmpty()) return "#반려동물";
        return hashtags.get(random.nextInt(hashtags.size()));
    }
}