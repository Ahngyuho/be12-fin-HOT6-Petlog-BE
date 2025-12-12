package com.hot6.backend.init;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hot6.backend.chat.model.ChatRoomUserMetaData;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Profile("local")
@RequiredArgsConstructor
//@Component
@Slf4j
public class InitJdbcDB {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final PetlogNameProvider petlogNameProvider;
    private final ChatRoomTitleProvider chatRoomTitleProvider;
    private final ChatMessageProvider chatMessageProvider;
    private final ScheduleMessageProvider scheduleMessageProvider;
    private final ChatRoomHashtagProvider chatRoomHashtagProvider;


    @PostConstruct
    public void init() {
        init(500, 3, 20000, 20, 10, 4000);
    }

    @Transactional
    public void init(int userCnt, int avgPets,
                     int roomCnt, int avgParts,
                     int avgSchedules, int avgMsgs) {
        createCategories();
        List<Long> userIds                = insertUsers(userCnt);
        Map<Long,List<Long>> userPets     = insertPets(userIds, avgPets);
        List<Long> roomIds                = insertChatRooms(roomCnt);
        Map<Long,List<Long>> partiIdxByRoom = insertChatRoomParticipants(roomIds, userIds, avgParts);
        insertSchedules(roomIds, partiIdxByRoom, userPets, avgSchedules);
//        insertChatMessagesWithOptimizedMeta(partiIdxByRoom, avgMsgs);
        insertChatMessagesFullyBatch(partiIdxByRoom, avgMsgs);
        insertHashtags(roomIds, 10);
    }

    private void createCategories() {
        // 카운트가 0일 때만 생성
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM category",
                Integer.class
        );
        if (count != null && count == 0) {
            List<Object[]> params = List.of(
                    new Object[]{"병원", "#00C9CD", "SCHEDULE"},
                    new Object[]{"미용", "#E6B0BD", "SCHEDULE"},
                    new Object[]{"산책", "#65924D", "SCHEDULE"},
                    new Object[]{"체중", "#00C9CD", "DAILY_RECORD"},
                    new Object[]{"이상현상", "#B36063", "DAILY_RECORD"}
            );

            jdbcTemplate.batchUpdate(
                    "INSERT INTO category (name, color, type) VALUES (?, ?, ?)",
                    params
            );
        }
    }

    private List<Long> insertUsers(int count) {
        long start = System.currentTimeMillis();
        log.info("⏳ [START] insertUsers - count = {}", count);

        List<Object[]> batch = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            batch.add(new Object[]{
                    "test" + i + "@test.com",
                    "$2a$10$.QJ.leSKCQXX9Tn8pCipIOy8F.XhB8o0Gl1AFIRBN10L0LCFiJSB2",
                    petlogNameProvider.getRandomNickname() + i,
                    "https://example.com/img" + i + ".png",
                    true,
                    false,
                    "USER"
            });
        }

        jdbcTemplate.batchUpdate(
                "INSERT INTO `user` (email, password, nickname, user_profile_image, enabled, is_deleted, user_type) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                batch
        );

        log.info("✅ [END] insertUsers - {}ms", System.currentTimeMillis() - start);

        return new ArrayList<>(
                jdbcTemplate.query(
                        "SELECT idx FROM `user` ORDER BY idx DESC LIMIT ?",
                        new Object[]{count},
                        (rs, rowNum) -> rs.getLong("idx")
                ).stream().sorted().toList()
        );
    }
    private Map<Long, List<Long>> insertPets(List<Long> userIds, int avgPets) {
        long start = System.currentTimeMillis();
        log.info("✅ [END] insertPets - {}ms", System.currentTimeMillis() - start);

        Map<Long, List<Long>> result = new HashMap<>();
        List<Object[]> batch = new ArrayList<>();
        Random rnd = new Random();

        for (Long userId : userIds) {
            int cnt = rnd.nextInt(avgPets) + 1;
            List<Long> petPlaceholders = new ArrayList<>();
            for (int i = 0; i < cnt; i++) {
                batch.add(new Object[]{
                        petlogNameProvider.getRandomPetName(),
                        Timestamp.valueOf(LocalDateTime.now().minusYears(2)),
                        userId,
                        true
                });
                petPlaceholders.add(-1L); // 나중에 PK 매핑할 때 쓸 자리
            }
            result.put(userId, petPlaceholders);
        }

        jdbcTemplate.batchUpdate(
                "INSERT INTO pet (name, birth_date, user_id, is_neutering) VALUES (?, ?, ?, ?)",
                batch
        );

        // PK 한 번에 조회 (최근 삽입된 수만큼만)
        int totalCount = batch.size();
        List<Long> petIds = jdbcTemplate.query(
                "SELECT idx FROM pet ORDER BY idx DESC LIMIT ?",
                new Object[]{totalCount},
                (rs, rowNum) -> rs.getLong("idx")
        );
        Collections.reverse(petIds); // 역순 보정

        // 결과에 다시 반영
        Iterator<Long> idIterator = petIds.iterator();
        for (Map.Entry<Long, List<Long>> entry : result.entrySet()) {
            List<Long> filled = new ArrayList<>();
            for (int i = 0; i < entry.getValue().size(); i++) {
                if (idIterator.hasNext()) filled.add(idIterator.next());
            }
            entry.setValue(filled);
        }


        log.info("✅ [END] insertPets - {}ms", System.currentTimeMillis() - start);

        return result;
    }
    private List<Long> insertChatRooms(int count) {
        long start = System.currentTimeMillis();
        log.info("⏳ [START] insertChatRooms");
        List<Object[]> batch = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            batch.add(new Object[]{
                    chatRoomTitleProvider.getRandomTitle(),
                    100
            });
        }

        jdbcTemplate.batchUpdate(
                "INSERT INTO chat_room (c_title, max_participants) VALUES (?, ?)",
                batch
        );

        List<Long> roomIds = jdbcTemplate.query(
                "SELECT idx FROM chat_room ORDER BY idx DESC LIMIT ?",
                new Object[]{count},
                (rs, rowNum) -> rs.getLong("idx")
        );
        Collections.reverse(roomIds);
        log.info("✅ [END] insertChatRooms - {}ms", System.currentTimeMillis() - start);

        return roomIds;
    }
    /* ---------- 4. PARTICIPANT ---------- */
    /**
     * 반환: roomId → 참가자(pk) 목록  
     * (나중에 chat FK 로 써야 하므로 userIdx 가 아니라 participantIdx!)
     */
    private Map<Long,List<Long>> insertChatRoomParticipants(
            List<Long> roomIds, List<Long> userIds, int avgParts) {

        long start = System.currentTimeMillis();
        log.info("⏳ [START] insertChatRoomParticipants");
        Random rnd = new Random();
        Map<Long,List<Long>> result = new HashMap<>();
        List<Object[]> batch = new ArrayList<>();

        // 1) 일단 INSERT 에 필요한 파라미터만 모두 모은다
        for (Long roomId : roomIds) {
            Collections.shuffle(userIds);
            int cnt = rnd.nextInt(avgParts) + 1;
            boolean adminDone = false;

            for (int i = 0; i < cnt; i++) {
                Long userIdx = userIds.get(i);
                batch.add(new Object[]{ userIdx, roomId, !adminDone,false });
                adminDone = true;
            }
        }

        // 2) 대량 삽입
        jdbcTemplate.batchUpdate(
                "INSERT INTO chat_room_participant (user_idx, chat_room_idx, is_admin,is_deleted) VALUES (?, ?, ?,?)",
                batch
        );
        // 3) 방별 PK를 한번에 가져오기
        jdbcTemplate.query(
                "SELECT idx, chat_room_idx FROM chat_room_participant",
                rs -> {
                    long idx   = rs.getLong("idx");
                    long room  = rs.getLong("chat_room_idx");
                    result.computeIfAbsent(room, k -> new ArrayList<>()).add(idx);
                });
        log.info("✅ [END] insertChatRoomParticipants - {}ms", System.currentTimeMillis() - start);

        return result;
    }
    private void insertChatMessagesWithOptimizedMeta(Map<Long, List<Long>> partiIdxByRoom, int avgMsgs) {
        Random rnd = new Random();
        final int BATCH_SIZE = 1000;
        LocalDateTime now = LocalDateTime.now();
        List<Object[]> currentBatch = new ArrayList<>();
        Map<Long, ChatRoomUserMetaData> updatedMetaMap = new HashMap<>();

        // 1. 전체 participant 메타데이터 미리 로딩
        Map<Long, ChatRoomUserMetaData> metaMap = jdbcTemplate.query(
                "SELECT idx, meta_data FROM chat_room_participant",
                rs -> {
                    Map<Long, ChatRoomUserMetaData> map = new HashMap<>();
                    while (rs.next()) {
                        Long idx = rs.getLong("idx");
                        String json = rs.getString("meta_data");
                        ChatRoomUserMetaData meta;
                        if (json != null) {
                            try {
                                meta = objectMapper.readValue(json, ChatRoomUserMetaData.class);
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to parse meta_data for participant " + idx, e);
                            }
                        } else {
                            meta = new ChatRoomUserMetaData();
                        }
                        map.put(idx, meta);
                    }
                    return map;
                }
        );

        for (Map.Entry<Long, List<Long>> entry : partiIdxByRoom.entrySet()) {
            List<Long> partList = entry.getValue();
            if (partList.isEmpty()) continue;

            int messageCount = rnd.nextInt(avgMsgs) + 100;

            for (int i = 0; i < messageCount; i++) {
                Long partiIdx = partList.get(rnd.nextInt(partList.size()));
                String message = "메시지-" + i;
                ChatRoomUserMetaData meta = metaMap.get(partiIdx);

                if (meta.getFirstJoinMessageId() == null) {
                    // 첫 메시지 insert
                    jdbcTemplate.update(
                            "INSERT INTO chat (chat_room_parti_idx, message, c_is_read, type, created_at) VALUES (?, ?, ?, ?, ?)",
                            partiIdx, message, false, "TEXT", Timestamp.valueOf(now)
                    );

                    // 해당 참가자의 가장 마지막 메시지 ID를 가져옴 (방금 insert된 ID)
                    Long newMessageId = jdbcTemplate.queryForObject(
                            "SELECT MAX(idx) FROM chat WHERE chat_room_parti_idx = ?",
                            Long.class,
                            partiIdx
                    );

                    meta.setFirstJoinMessageId(newMessageId);
                    meta.setLastSeenMessageId(newMessageId);
                    meta.setJoinedAt(now);
                    meta.setMuted(false);
                    meta.setNotificationsEnabled(true);
                    updatedMetaMap.put(partiIdx, meta);
                } else {
                    // 이후 메시지는 배치 insert
                    currentBatch.add(new Object[]{
                            partiIdx, message, false, "TEXT", Timestamp.valueOf(now)
                    });

                    if (currentBatch.size() >= BATCH_SIZE) {
                        jdbcTemplate.batchUpdate(
                                "INSERT INTO chat (chat_room_parti_idx, message, c_is_read, type, created_at) VALUES (?, ?, ?, ?, ?)",
                                currentBatch
                        );
                        currentBatch.clear();
                    }
                }
            }
        }

        // 마지막 남은 메시지 저장
        if (!currentBatch.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "INSERT INTO chat (chat_room_parti_idx, message, c_is_read, type, created_at) VALUES (?, ?, ?, ?, ?)",
                    currentBatch
            );
        }

        // 메타데이터 일괄 업데이트
        for (Map.Entry<Long, ChatRoomUserMetaData> entry : updatedMetaMap.entrySet()) {
            try {
                String json = objectMapper.writeValueAsString(entry.getValue());
                jdbcTemplate.update(
                        "UPDATE chat_room_participant SET meta_data = ? WHERE idx = ?",
                        json, entry.getKey()
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize meta_data for participant " + entry.getKey(), e);
            }
        }
    }
    private static <T> List<List<T>> partition(List<T> list, int batchSize) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            result.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return result;
    }

    private void insertChatMessagesFullyBatch(Map<Long, List<Long>> partiIdxByRoom, int avgMsgs) {
        long start = System.currentTimeMillis();
        log.info("⏳ [START] insertChatMessagesFullyBatch");
        Random rnd = new Random();
        final int BATCH_SIZE = 1000;
        List<Object[]> messageBatch = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 1. 전체 meta_data 로딩
        Map<Long, ChatRoomUserMetaData> metaMap = jdbcTemplate.query(
                "SELECT idx, meta_data FROM chat_room_participant",
                rs -> {
                    Map<Long, ChatRoomUserMetaData> map = new HashMap<>();
                    while (rs.next()) {
                        Long idx = rs.getLong("idx");
                        String json = rs.getString("meta_data");
                        ChatRoomUserMetaData meta;
                        if (json != null) {
                            try {
                                meta = objectMapper.readValue(json, ChatRoomUserMetaData.class);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException("❌ Failed to parse meta_data JSON for participant " + idx, e);
                            }
                        } else {
                            meta = new ChatRoomUserMetaData();
                        }
                        map.put(idx, meta);
                    }
                    return map;
                }
        );

        // 2. 전체 메시지 준비
        Set<Long> firstMessageCandidates = new HashSet<>();
        for (Map.Entry<Long, List<Long>> entry : partiIdxByRoom.entrySet()) {
            List<Long> partList = entry.getValue();
            if (partList.isEmpty()) continue;

            int messageCount = rnd.nextInt(avgMsgs) + 100;

            for (int i = 0; i < messageCount; i++) {
                Long partiIdx = partList.get(rnd.nextInt(partList.size()));
                ChatRoomUserMetaData meta = metaMap.get(partiIdx);

                if (meta.getFirstJoinMessageId() == null) {
                    firstMessageCandidates.add(partiIdx);
                }

                messageBatch.add(new Object[]{
                        partiIdx,
                        chatMessageProvider.getRandomMessage(),
                        false,
                        "TEXT",
                        Timestamp.valueOf(now)
                });
            }
        }

        // 3. 배치 insert
        List<List<Object[]>> batches = partition(messageBatch, BATCH_SIZE);
        for (List<Object[]> batch : batches) {
            jdbcTemplate.batchUpdate(
                    "INSERT INTO chat (chat_room_parti_idx, message, c_is_read, type, created_at) VALUES (?, ?, ?, ?, ?)",
                    batch
            );
        }

        // 4. 첫 메시지 ID들 조회
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("ids", new ArrayList<>(firstMessageCandidates));

        Map<Long, Long> firstMessageIdMap = namedParameterJdbcTemplate.query(
                "SELECT chat_room_parti_idx, MIN(idx) as first_id FROM chat WHERE chat_room_parti_idx IN (:ids) GROUP BY chat_room_parti_idx",
                paramSource,
                rs -> {
                    Map<Long, Long> map = new HashMap<>();
                    while (rs.next()) {
                        map.put(rs.getLong("chat_room_parti_idx"), rs.getLong("first_id"));
                    }
                    return map;
                }
        );

        // 5. 메타데이터 업데이트
        for (Map.Entry<Long, Long> entry : firstMessageIdMap.entrySet()) {
            Long partiIdx = entry.getKey();
            Long messageId = entry.getValue();
            ChatRoomUserMetaData meta = metaMap.get(partiIdx);
            meta.setFirstJoinMessageId(messageId);
            meta.setLastSeenMessageId(messageId);
            meta.setJoinedAt(now);
            meta.setMuted(false);
            meta.setNotificationsEnabled(true);
        }

        // 6. 메타데이터 저장
        for (Map.Entry<Long, ChatRoomUserMetaData> entry : metaMap.entrySet()) {
            if (entry.getValue().getFirstJoinMessageId() != null) {
                try {
                    String json = objectMapper.writeValueAsString(entry.getValue());
                    jdbcTemplate.update(
                            "UPDATE chat_room_participant SET meta_data = ? WHERE idx = ?",
                            json, entry.getKey()
                    );
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("❌ Failed to serialize meta_data for participant " + entry.getKey(), e);
                }
            }
        }


        // 메모리 로그 + GC 힌트
        long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        log.info("🧹 Clearing temp objects & suggesting GC... Used memory: {} MB", usedMem);

        // Optional: System.gc(); // ← 디버깅 전용


        log.info("✅ [END] insertChatMessagesFullyBatch - {}ms", System.currentTimeMillis() - start);

    }
    private void insertSchedules(List<Long> roomIds, Map<Long, List<Long>> roomParticipants, Map<Long, List<Long>> petMap, int avgSchedules) {
        long start = System.currentTimeMillis();
        log.info("⏳ [START] insertSchedules");
        Random rand = new Random();
        List<Object[]> scheduleBatch = new ArrayList<>();
        List<Object[]> sharedBatch = new ArrayList<>();

        // 1. schedule 개수 및 값 생성
        List<Integer> scheduleCounts = new ArrayList<>();
        int totalSchedules = 0;

        for (Long roomId : roomIds) {
            List<Long> participants = roomParticipants.get(roomId);
            if (participants == null || participants.isEmpty()) {
                scheduleCounts.add(0);
                continue;
            }

            int count = rand.nextInt(avgSchedules) + 1;
            scheduleCounts.add(count);
            totalSchedules += count;

            for (int i = 0; i < count; i++) {
                scheduleBatch.add(new Object[]{
                        roomId,
                        scheduleMessageProvider.getRandomTitle(),                     // s_title
                        scheduleMessageProvider.getRandomMemo(),                 // s_memo
                        false,                                           // recurring
                        null,                                            // repeat_cycle
                        scheduleMessageProvider.getRandomPlace(),                   // place_id
                        false,                                           // is_deleted
                        0,                                               // repeat_count
                        null,                                            // repeat_end_at
                        Timestamp.valueOf(LocalDateTime.now()),         // start_at
                        Timestamp.valueOf(LocalDateTime.now().plusHours(1)), // end_at
                        rand.nextInt(5) + 1                              // category_idx (1~5)
                });
            }
        }

        // 2. INSERT schedule
        jdbcTemplate.batchUpdate(
                "INSERT INTO schedule (chat_room_idx, s_title, s_memo, recurring, repeat_cycle, place_id, is_deleted, repeat_count, repeat_end_at, start_at, end_at, category_idx) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                scheduleBatch
        );

        // 3. schedule_id 조회
        List<Long> scheduleIds = jdbcTemplate.query(
                "SELECT idx FROM schedule ORDER BY idx DESC LIMIT ?",
                new Object[]{totalSchedules},
                (rs, rowNum) -> rs.getLong("idx")
        );
        Collections.reverse(scheduleIds);  // 순서 맞추기

        // 4. shared_schedule_pet 생성
        int scheduleCursor = 0;
        for (int i = 0; i < roomIds.size(); i++) {
            Long roomId = roomIds.get(i);
            int count = scheduleCounts.get(i);
            List<Long> participants = roomParticipants.get(roomId);
            if (participants == null || participants.isEmpty()) continue;

            for (int j = 0; j < count; j++) {
                Long scheduleId = scheduleIds.get(scheduleCursor++);
                Long creator = participants.get(rand.nextInt(participants.size()));
                List<Long> pets = petMap.getOrDefault(creator, Collections.emptyList());
                Collections.shuffle(pets);
                for (int k = 0; k < Math.min(pets.size(), 2); k++) {
                    sharedBatch.add(new Object[]{scheduleId, pets.get(k)});
                }
            }
        }

        // 5. INSERT shared_schedule_pet
        jdbcTemplate.batchUpdate(
                "INSERT INTO shared_schedule_pet (schedule_idx, pet_idx) VALUES (?, ?)",
                sharedBatch
        );

        log.info("✅ [END] insertSchedules - {}ms", System.currentTimeMillis() - start);

    }

    private void insertHashtags(List<Long> roomIds, int countPerRoom) {
        long start = System.currentTimeMillis();
        log.info("⏳ [START] insertHashtags");
        Random rand = new Random();
        List<Object[]> batch = new ArrayList<>();
        for (Long roomId : roomIds) {
            for (int i = 0; i < countPerRoom; i++) {
                batch.add(new Object[]{roomId, chatRoomHashtagProvider.getRandomHashtag()});
            }
        }
        jdbcTemplate.batchUpdate("INSERT INTO chat_room_hashtag (chat_room_idx, c_tag) VALUES (?, ?)", batch);
        log.info("✅ [END] insertHashtags - {}ms", System.currentTimeMillis() - start);

    }
}

