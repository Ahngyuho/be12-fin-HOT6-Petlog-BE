package com.hot6.backend.chat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hot6.backend.category.model.Category;
import com.hot6.backend.pet.model.Pet;
import com.hot6.backend.schedule.model.Schedule;
import com.hot6.backend.user.model.User;
import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ChatDto {
    @Getter
    @Builder
//    @AllArgsConstructor
    @Schema(description = "채팅방 생성 요청 DTO")
    public static class CreateChatRoomRequest {
        @Schema(description = "채팅방 제목", example = "햄스터 친구 구해요")
        private final String title;

        @Schema(description = "채팅방 해시태그 목록", example = "[\"#햄스터\", \"#김포\", \"#친구\"]")
        private final List<String> hashtags;

        @Schema(description = "채팅방 시작 시간", example = "2025-05-13T13:21:00")
        private final String startDateTime;

        @Schema(description = "채팅방 최대 참여 인원", example = "50")
        private final Integer maxParticipants; // 최대 인원 추가
    }

    @Getter
    @Schema(description = "채팅방 수정 요청 DTO")
    public static class UpdateChatRequest {
        @Schema(description = "변경할 채팅방 제목", example = "햄스터 사육 정보 공유방")
        private String title;

        @Schema(description = "수정할 해시태그 목록", example = "[\"#햄스터\", \"#정보\"]")
        private List<String> hashtags;
    }

    @Getter
    @Builder
    @Schema(description = "채팅방 정보 응답 DTO")
    public static class ChatRoomListDto {
        @Schema(description = "채팅방 idx", example = "1")
        public Long idx;

        @Schema(description = "채팅방 제목", example = "햄스터 친구 구해요")
        public String title;

        @Schema(description = "채팅방 해시태그", example = "[\"#햄스터\", \"#친구\"]")
        public List<String> hashtags;

        @Schema(description = "채팅방 참여 인원수", example = "6")
        public int participants;

        public int maxParticipants;

        @Schema(description = "참여 여부", example = "true")
        public Boolean isParticipating;


        public LocalDateTime startDateTime;

        public static ChatRoomListDto from(ChatRoom chatRoom, Long userIdx) {
            return ChatRoomListDto.builder()
                    .idx(chatRoom.getIdx())
                    .title(chatRoom.getCTitle())
                    .startDateTime(chatRoom.getStartDateTime())
                    .maxParticipants(chatRoom.getMaxParticipants())
                    .participants(chatRoom.getCurrentParticipants())
                    .hashtags(chatRoom.getHashtags().stream().map(ChatRoomHashtag::getCTag).collect(Collectors.toList()))
                    .isParticipating(
                            userIdx != null && chatRoom.getParticipants().stream()
                            .anyMatch(participant -> participant.getUser().getIdx().equals(userIdx)))
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "채팅방 정보 응답 DTO")
    public static class MyChatRoomListDto {
        @Schema(description = "채팅방 idx", example = "1")
        public Long idx;

        @Schema(description = "채팅방 제목", example = "햄스터 친구 구해요")
        public String title;

        @Schema(description = "채팅방 해시태그", example = "[\"#햄스터\", \"#친구\"]")
        public List<String> hashtags;

        @Schema(description = "채팅방 참여 인원수", example = "6")
        public int participants;

        @Schema(description = "참여 여부", example = "true")
        public Boolean isParticipating;

        public static MyChatRoomListDto from(ChatRoom chatRoom) {
            return MyChatRoomListDto.builder()
                    .idx(chatRoom.getIdx())
                    .title(chatRoom.getCTitle())
                    .participants(chatRoom.getCurrentParticipants())
                    .hashtags(chatRoom.getHashtags().stream().map(ChatRoomHashtag::getCTag).collect(Collectors.toList()))
                    .isParticipating(true)
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "채팅방 정보 응답 DTO")
    public static class ChatRoomDetailInfo {
        @Schema(description = "채팅방 idx", example = "1")
        public Long idx;

        @Schema(description = "채팅방 제목", example = "햄스터 친구 구해요")
        public String title;

        @Schema(description = "채팅방 해시태그", example = "[\"#햄스터\", \"#친구\"]")
        public List<String> hashtags;

        @Schema(description = "채팅방 주인", example = "true")
        public boolean isAdmin;

        @Schema(description = "채팅방 참여 인원수", example = "6")
        public int participants;

        public static ChatRoomDetailInfo from(ChatRoom chatRoom,Long userIdx) {
            return ChatRoomDetailInfo.builder()
                    .idx(chatRoom.getIdx())
                    .title(chatRoom.getCTitle())
                    .participants(chatRoom.getCurrentParticipants())
                    .hashtags(chatRoom.getHashtags().stream().map(ChatRoomHashtag::getCTag).collect(Collectors.toList()))
                    .isAdmin(
                            chatRoom.getParticipants().stream()
                                    .anyMatch(p -> p.getUser().getIdx().equals(userIdx) && p.getIsAdmin())
                    )
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "채팅 메시지 응답 DTO")
    public static class ChatElement {
        @Schema(description = "채팅 고유 idx", example = "1")
        public Long idx;

        @Schema(description = "채팅 메시지 내용", example = "안녕하세요~")
        private Object content;

        @Schema(description = "보낸 사용자 idx", example = "1")
        public Long senderIdx;

        @Schema(description = "보낸 사용자 닉네임", example = "hamster_lover")
        public String nickname;

        @Schema(description = "메시지 타입", example = "text")
        private ChatMessageType type;

        @Schema(description = "보낸 시간", example = "2025-04-07T12:34:56")
        public String createdAt;


        public static ChatElement from(Chat chat) {
            ObjectMapper mapper = new ObjectMapper();
            Object contentObj;
            try {
                switch (chat.getType()) {
                    case TEXT -> {
                        contentObj = chat.getMessage();
                    }
                    case PET -> {
                        contentObj = mapper.readValue(chat.getMessage(), PetContent.class);
                    }
                    case SCHEDULE -> {
                        contentObj = mapper.readValue(chat.getMessage(), ScheduleContent.class);
                    }
                    default -> throw new IllegalArgumentException("알 수 없는 타입: " + chat.getType());
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException("메시지 역직렬화 실패", e);
            }
            return ChatElement.builder()
                    .idx(chat.getIdx())
                    .senderIdx(chat.getChatRoomParticipant().getUser().getIdx())
                    .nickname(chat.getChatRoomParticipant().getUser().getNickname())
                    .createdAt(chat.getCreatedAt().toString())
                    .content(contentObj)
                    .type(chat.getType())
                    .build();
        }
    }

    @Getter
    @Schema(description = "채팅방 검색 요청 DTO (POST 방식용)")
    public static class ChatSearchRequest {
        @Schema(description = "검색어", example = "햄스터")
        public String query;

        @Schema(description = "검색할 해시태그 목록", example = "[\"#햄스터\"]")
        public List<String> hashtags;
    }

    @Getter
    @Schema(description = "채팅 메시지 전송 요청 DTO")
    public static class CreateChatRequest {
        @Schema(description = "채팅 메시지", example = "반가워요!")
        public String message;

        @Schema(description = "보낸 사용자 닉네임", example = "user01")
        public String nickname;

        @Schema(description = "채팅방 idx", example = "1")
        public Long chatRoomIdx;

        @Schema(description = "보낸 시간", example = "2025-04-07T12:00:00")
        public String createdAt;
    }

    @Getter
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageDto {
        private Long chatroomId;
        private ChatContent content;        // 본문 내용 or Base64 or URL
        private String timestamp;   // ISO String (정렬용, 표시용)
    }

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY, // content 내부에 타입 정보 포함,
            property = "type",
            visible = true
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = TextContent.class, name = "text"),
            @JsonSubTypes.Type(value = PetContent.class, name = "pet"),
            @JsonSubTypes.Type(value = ScheduleContent.class, name = "schedule")
    })
    public interface ChatContent {
        // marker interface
        String getType();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class TextContent implements ChatContent {
        private String message;

        @Override
        public String getType() {
            return "text";
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PetContent implements ChatContent {
        private Long idx;
        private String name;
        private String breed;
        private String gender;
        private Integer age;
        private String image;

        @Override
        public String getType() {
            return "pet";
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScheduleContent  implements ChatContent {
        private String title;
        private String datetime;
        private String location;

        @Override
        public String getType() {
            return "schedule";
        }
    }

    @Getter
    public static class ChatUserInfo {
        @Schema(description = "채팅방 idx", example = "1")
        public Long idx;

        @Schema(description = "채팅방에 참여한 유저 닉네임", example = "User1")
        public String userName;

        @Schema(description = "채팅방에 참여한 유저의 imageUrl", example = "User1")
        public String imageUrl;

        @QueryProjection
        public ChatUserInfo(Long idx, String userName, String imageUrl) {
            this.idx = idx;
            this.userName = userName;
            this.imageUrl = imageUrl;
        }
    }

    @Getter
    @Builder
    public static class ChatRoomScheduleElement {
        @Schema(description = "채팅방 스케쥴 idx", example = "1")
        public Long idx;

        @Schema(description = "지정 일정의 시작 시간", example = "25.04.12 11:00")
        public String time;

        @Schema(description = "지정 채팅방의 제목" , example = "User1")
        public String title;

        public static ChatRoomScheduleElement from(Schedule schedule){
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy.MM.dd HH:mm");
            return ChatRoomScheduleElement.builder()
                    .idx(schedule.getIdx())
                    .time(schedule.getStartAt().format(formatter))
                    .title(schedule.getSTitle())
                    .build();
        }
    }


    @Getter
    @NoArgsConstructor
    @Schema(description = "채팅방 일정 생성 요청 DTO")
    public static class CreateChatRoomScheduleRequest {

        @Schema(description = "일정 제목", example = "예방접종")
        private String title;

        @Schema(description = "일정 시작 시간", example = "2025-04-10T10:00:00")
        private LocalDateTime startAt;

        @Schema(description = "일정 종료 시간", example = "2025-04-10T11:00:00")
        private LocalDateTime endAt;

        @Schema(description = "장소 ID", example = "병원")
        private String placeId;

        @Schema(description = "메모", example = "서울 동물병원에서 예방접종")
        private String memo;

        @Schema(description = "카테고리 ID", example = "1")
        private Long categoryIdx;

        // 엔티티로 변환하는 편의 메서드
        public Schedule toEntity(ChatRoom chatRoom, Category category) {
            return Schedule.builder()
                    .sTitle(title)
                    .sMemo(memo)
                    .placeId(placeId)
                    .startAt(startAt)
                    .endAt(endAt)
                    .categoryIdx(category.getIdx())
                    .isDeleted(false)
                    .recurring(false) // 현재는 반복 설정이 없는 구조
                    .chatRoom(chatRoom)
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "채팅방 일정 상세 조회 응답 DTO")
    public static class ChatRoomScheduleDetailResponse {

        @Schema(description = "일정 제목")
        private String title;

        @Schema(description = "일정 시간 범위", example = "2025-04-20T14:00:00 ~ 2025-04-20T15:00:00")
        private String time;

        @Schema(description = "일정 장소")
        private String place;

        @Schema(description = "일정 메모")
        private String memo;

        @Schema(description = "채팅방 참여자 리스트")
        private List<ChatUserInfo> participants;

        @Schema(description = "현재 사용자의 참여 여부")
        private boolean isParticipating;

        @Schema(description = "내 반려동물 목록")
        private List<PetInChatRoomSchedule> pets;

        public static ChatRoomScheduleDetailResponse from(Schedule schedule, List<User> users, boolean isParticipating ,List<Pet> pets){
            return ChatRoomScheduleDetailResponse.builder()
                    .title(schedule.getSTitle())
                    .time(schedule.getStartAt().format(DateTimeFormatter.ofPattern("yy.MM.dd HH:mm")))
                    .place(schedule.getPlaceId())
                    .memo(schedule.getSMemo())
                    .participants(users.stream()
                            .map(u -> new ChatUserInfo(
                                    u.getIdx(),
                                    u.getNickname(),
                                    u.getUserProfileImage()
                            ))
                            .collect(Collectors.toList()))
                    .isParticipating(isParticipating)
                    .pets(pets.stream().map(PetInChatRoomSchedule::from).collect(Collectors.toList()))
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "현재 채팅방에 참여하고 있는 사용자의 반려동물들 리스트 DTO")
    public static class PetInChatRoomSchedule{
        @Schema(description = "반려 동물 idx")
        private Long idx;

        @Schema(description = "반려 동물 이름")
        private String petName;

        public static PetInChatRoomSchedule from(Pet pet){
            return PetInChatRoomSchedule.builder()
                    .idx(pet.getIdx())
                    .petName(pet.getName())
                    .build();
        }
    }

    @Getter
    @Schema
    public static class ParticipateChatRoomSchedule {
        List<Long> animalIds;
    }
}
