package com.hot6.backend.user.model;

import com.hot6.backend.board.answer.model.Answer;
import com.hot6.backend.board.question.model.Question;
import com.hot6.backend.chat.model.ChatRoomParticipant;
import com.hot6.backend.schedule.model.Schedule;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
@Builder
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    private String email;
    private String password;
    private String nickname;
    private String userProfileImage;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType userType;
//    private boolean enabled;

    private String provider;
    private Long providerId;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "user")
    private List<ChatRoomParticipant> chatParticipations = new ArrayList<>();

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        GrantedAuthority authority = new SimpleGrantedAuthority(userType.toString());

        authorities.add(authority);
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return nickname;
    }

//    public void userVerify() {
//        this.enabled = true;
//    }

//    public boolean getEnabled() {
//        return this.enabled;
//    }


    @OneToMany(mappedBy = "user")
    private List<Question> questionList = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Answer> answerList = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private EmailVerify emailVerify;
}
