package com.hot6.backend.user.model;

import com.hot6.backend.pet.model.PetDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class UserDto {

    @Getter
    @Builder
    @Schema(description = "회원가입 요청 DTO")
    public static class CreateRequest {

        @Schema(description = "이메일", example = "user@example.com")
        @Email(message = "올바른 이메일 형식을 입력해주세요.")
        @NotBlank(message = "이메일은 필수 입력 사항입니다.")
        private String email;

        @Schema(description = "비밀번호 (영어, 숫자, 특수문자 포함 8자 이상)", example = "Password123!")
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자 사이여야 합니다.")
        @NotBlank(message = "비밀번호 필수 입력 사항입니다.")
        private String password;

        @Schema(description = "닉네임(자음/모음, 특수문자 불가)", example = "happyDogLover")
        @Size(min = 2, max = 16, message = "닉네임은 최소 2자에서 16자까지 입력 가능합니다.")
        @NotBlank(message = "닉네임은 필수 입력 사항입니다.")
        private String nickname;

        @Schema(description = "프로필 이미지 URL (선택사항)", example = "https://example.com/image.png")
        private String profileImageUrl;

        @Schema(description = "회원 역할 (USER or ADMIN)", example = "USER")
        private String role;

        @Schema(description = "이메일 인증 여부", example = "false")
        private boolean enabled;

        @Schema(description = "로그인 구분", example = "null")
        private String provider;

        @Schema(description = "소셜 id", example = "1234")
        private Long providerId;


        public User toEntity(String encodedPassword, String resolvedProfileImageUrl) {
            return User.builder()
                    .email(email)
                    .password(encodedPassword)
                    .nickname(nickname)
                    .userType(UserType.valueOf(role))
                    .userProfileImage(resolvedProfileImageUrl)
//                    .enabled(enabled)
                    .isDeleted(false)
                    .provider(provider)
                    .providerId(providerId)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class CreateResponse {
        @Schema(description = "회원가입된 이메일", example = "user@example.com")
        private String email;

        public static CreateResponse from(User user) {
            return CreateResponse.builder().email(user.getEmail()).build();
        }
    }

    @Getter
    public static class SocialRegisterRequest {
        @Schema(description = "소셜 이메일", example = "kakao_user@example.com")
        private String email;

        @Schema(description = "소셜 닉네임", example = "카카오유저")
        private String nickname;

        @Schema(description = "소셜 프로필 이미지", example = "https://kakao.com/user/profile.jpg")
        private String profileImageUrl;

        @Schema(description = "소셜 제공자 (예: kakao)", example = "kakao")
        private String socialProvider;

        @Schema(description = "소셜 액세스 토큰", example = "abcdefg1234567")
        private String accessToken;
    }

    @Getter
    public static class LoginRequest {
        @Schema(description = "로그인 이메일", example = "user@example.com")
        private String email;

        @Schema(description = "로그인 비밀번호", example = "Password123!")
        private String password;
    }

    @Getter
    @Builder
    public static class LoginResponse {
        @Schema(description = "JWT 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIs...")
        private String accessToken;
    }

    @Getter
    @Builder
    public static class UserProfileResponse {
        @Schema(description = "이메일", example = "user@example.com")
        private String email;

        @Schema(description = "닉네임", example = "happyDogLover")
        private String nickname;

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/image.png")
        private String profileImageUrl;

        @Schema(description = "회원의 펫 카드 리스트")
        private List<PetDto.PetCard> petCards;

        @Schema(description = "카카오 로그인 여부")
        private String provider;
    }

    @Getter
    public static class UserProfileImageUpdateRequest {
        @Schema(description = "새 프로필 이미지 URL", example = "https://example.com/updated.png")
        private String profileImageUrl;
    }

    @Getter
    public static class PasswordUpdateRequest {
        @Schema(description = "현재 비밀번호", example = "CurrentPassword123!")
        private String currentPassword;

        @Schema(description = "새 비밀번호", example = "NewPassword456!")
        private String newPassword;
    }

    @Getter
    public static class UserDeleteRequest {
        @Schema(description = "회원 이메일", example = "user@example.com")
        private String email;

        @Schema(description = "회원 비밀번호", example = "Password123!")
        private String password;
    }

    @Getter
    @Builder
    public static class LoginCheckResponse {
        @Schema(description = "회원 idx", example = "1")
        private Long idx;

        @Schema(description = "로그인 여부", example = "true")
        private boolean isLogin;

        @Schema(description = "회원 닉네임", example = "user")
        private String nickname;
//
//        @Schema(description = "이메일 인증 여부", example = "false")
//        private boolean enabled;

        @Schema(description = "회원 역할 (USER or ADMIN)", example = "USER")
        private String role;

        public static LoginCheckResponse from(User user) {
            return LoginCheckResponse.builder()
                    .idx(user.getIdx())
                    .isLogin(true)
                    .nickname(user.getNickname())
//                    .enabled(user.isEnabled())
                    .role(String.valueOf(user.getUserType()))
                    .build();
        }
    }

    @Getter
    @Setter
    public static class NicknameUpdateRequest {
        private String newNickname;
    }
    @Getter
    @Builder
    @AllArgsConstructor
    public static class DeletedUserResponse {
        private Long idx;
        private String email;
        private String nickname;
        private String userProfileImage;
        private String userType;
        private Boolean isDeleted;

        public static DeletedUserResponse from(User user) {
            return DeletedUserResponse.builder()
                    .idx(user.getIdx())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .userProfileImage(user.getUserProfileImage())
                    .userType(user.getUserType().name())
                    .isDeleted(user.getIsDeleted())
                    .build();
        }
    }
}
