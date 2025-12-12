package com.hot6.backend.init;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

//@Component
public class PetlogNameProvider {

    private final List<String> nicknames;
    private final List<String> petNames;
    private final List<String> hashtags;
    private final Random random = new Random();

    public PetlogNameProvider() {
        this.nicknames = new ArrayList<>();
        this.petNames = new ArrayList<>();
        this.hashtags = new ArrayList<>();
        loadData();
    }

    private void loadData() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/static/petlog_names_and_tags.txt"))))) {
            List<String> target = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("[유저 닉네임]")) {
                    target = nicknames;
                } else if (line.startsWith("[반려동물 이름]")) {
                    target = petNames;
                } else if (line.startsWith("[해시태그 목록]")) {
                    target = hashtags;
                } else if (!line.trim().isEmpty() && target != null) {
                    target.add(line.trim());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("petlog_names_and_tags.txt 읽기 오류", e);
        }
    }

    public String getRandomNickname() {
        return nicknames.get(random.nextInt(nicknames.size()));
    }

    public String getRandomPetName() {
        return petNames.get(random.nextInt(petNames.size()));
    }

    public String getRandomHashtag() {
        return hashtags.get(random.nextInt(hashtags.size()));
    }
}
