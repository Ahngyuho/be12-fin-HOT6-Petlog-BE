package com.hot6.backend.chat.service;

import com.hot6.backend.chat.model.Chat;
import com.hot6.backend.chat.model.ChatRoom;
import com.hot6.backend.chat.model.ChatRoomParticipant;
import com.hot6.backend.chat.model.ChatRoomUserMetaData;
import com.hot6.backend.chat.repository.ChatMessageRepository;
import com.hot6.backend.chat.repository.ChatRoomParticipantRepository;
import com.hot6.backend.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

//>? 이게 뭔데 추가하라고 하지?
//이거 없으면 Mock 지정해놓은 객체에 접근하면 null
@ExtendWith(MockitoExtension.class) // JUnit5에서 Mockito 활성화
class ChatRoomParticipantServiceTest {
    @Mock
    ChatRoomParticipantRepository chatRoomParticipantRepository;
    @Mock
    ChatMessageService chatMessageService;
    @Mock
    ChatMessageRepository chatMessageRepository;

    @InjectMocks
    ChatRoomParticipantService service; // join() 메서드가 있는 서비스

    // ==== 헬퍼: 더미 엔티티 생성 ====
    private User user(long id) {
        User u = new User();
        u.setIdx(id);
        return u;
    }

    private ChatRoom room(long id) {
        return ChatRoom.builder()
                .idx(id)
                .build();
    }

    private Chat chat(long id) {
        return Chat.builder()
                .idx(id)
                .build();
    }

    @Test
    public void 채팅방에_참여할_때_정상적으로_참여() {
        // 채팅방에 정상적으로 참여하기 위해선
        // 우선 채팅방에 참여하려는 사용자의 id 를 받아서, 이 id 가 존재하는지 확인?
        // 채팅방 id 를 받아서 존재하는 채팅방인지 확인
        //

        // given
        User u = user(1L);
        ChatRoom r = room(100L);

        when(chatMessageService.findLatestChatByChatRoom(r))
                .thenReturn(Optional.empty());
        when(chatRoomParticipantRepository.findChatRoomParticipantIncludingDeleted(r.getIdx(), u.getIdx()))
                .thenReturn(Optional.empty());

        ArgumentCaptor<ChatRoomParticipant> captor = ArgumentCaptor.forClass(ChatRoomParticipant.class);
        // when
        LocalDateTime before = LocalDateTime.now();
        service.join(u, r);
        LocalDateTime after = LocalDateTime.now();


        // then
        verify(chatRoomParticipantRepository).save(captor.capture());
        ChatRoomParticipant saved = captor.getValue();


        assertThat(saved.getUser()).isSameAs(u);
        assertThat(saved.getChatRoom()).isSameAs(r);
        assertThat(saved.getIsAdmin()).isFalse(); // 신규는 false로 빌더 세팅

        ChatRoomUserMetaData md = saved.getMetaData();
        assertThat(md).isNotNull();
        assertThat(md.getFirstJoinMessageId()).isNull();
        assertThat(md.getLastSeenMessageId()).isNull();
        assertThat(md.isMuted()).isFalse();
        assertThat(md.isNotificationsEnabled()).isTrue();
        assertThat(md.getJoinedAt()).isNotNull();
        // joinedAt이 now 근처인지 (±2초)
        assertThat(Duration.between(before, md.getJoinedAt()).abs().getSeconds()).isLessThanOrEqualTo(2);
        assertThat(Duration.between(md.getJoinedAt(), after).abs().getSeconds()).isLessThanOrEqualTo(2);

        verify(chatRoomParticipantRepository, times(1))
                .findChatRoomParticipantIncludingDeleted(r.getIdx(), u.getIdx());
        verify(chatMessageService, times(1))
                .findLatestChatByChatRoom(r);
        verifyNoMoreInteractions(chatMessageService, chatRoomParticipantRepository);
    }
}