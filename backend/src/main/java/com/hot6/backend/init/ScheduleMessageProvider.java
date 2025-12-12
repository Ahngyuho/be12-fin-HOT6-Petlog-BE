package com.hot6.backend.init;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

//@Component
public class ScheduleMessageProvider {

    private final List<String> titles;
    private final List<String> memos;
    private final List<String> places;
    private final Random random = new Random();

    public ScheduleMessageProvider() {
        this.titles = load("/static/schedule_titles.txt");
        this.memos = load("/static/schedule_memos.txt");
        this.places = load("/static/schedule_places.txt");
    }

    private List<String> load(String path) {
        try (InputStream is = getClass().getResourceAsStream(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().map(String::trim).filter(l -> !l.isEmpty()).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("일정 메시지 로딩 오류 - " + path, e);
        }
    }

    public String getRandomTitle() {
        return titles.get(random.nextInt(titles.size()));
    }

    public String getRandomMemo() {
        return memos.get(random.nextInt(memos.size()));
    }

    public String getRandomPlace() {
        return places.get(random.nextInt(places.size()));
    }
}