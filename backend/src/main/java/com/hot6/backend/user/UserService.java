package com.hot6.backend.user;

import com.hot6.backend.common.BaseResponseStatus;
import com.hot6.backend.common.exception.BaseException;
import com.hot6.backend.pet.model.PetDto;
//import com.hot6.backend.redis.RefreshTokenRepository;
import com.hot6.backend.user.model.EmailVerify;
import com.hot6.backend.user.model.User;
import com.hot6.backend.user.model.UserDto;
import com.hot6.backend.utils.JwtUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final EmailVerifyRepository emailVerifyRepository;
//    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${profile-image}")
    private String defaultProfileImageUrl;

    @Value("${email-auth-url}")
    private String emailAuthUrl;

    @Value("${frontend-server}")
    private String frontendServer;

    @Transactional(readOnly = false)
    public void sendEmail(String uuid, String email) {
        System.out.println(email);
        String authUrl = emailAuthUrl + uuid;
        String htmlContent = """
                <div style="font-family: 'Apple SD Gothic Neo', 'sans-serif' !important; width: 540px; height: 600px; border-top: 4px solid #6A0104; margin: 100px auto; padding: 30px 0; box-sizing: border-box;">
                    <h1 style="margin: 0; padding: 0 5px; font-size: 28px; font-weight: 400;">
                        <span style="font-size: 15px; margin: 0 0 10px 3px;">펫로그</span><br />
                        <span style="color: #6A0104;">이메일 인증</span> 안내입니다.
                    </h1>
                    <p style="font-size: 16px; line-height: 26px; margin-top: 50px; padding: 0 5px;">
                        안녕하세요.<br />
                        펫로그에 가입해 주셔서 진심으로 감사드립니다.<br />
                        아래 <b style="color: #6A0104;">'메일 인증'</b> 버튼을 클릭하여 회원가입을 완료해 주세요.<br />
                        감사합니다.
                    </p>
                
                    <a style="color: #FFF; text-decoration: none; text-align: center;" href="%s" target="_blank">
                        <p style="display: inline-block; width: 210px; height: 45px; margin: 30px 5px 40px; background: #6A0104; line-height: 45px; vertical-align: middle; font-size: 16px;">메일 인증</p>
                    </a>
                
                    <div style="border-top: 1px solid #DDD; padding: 5px;">
                        <p style="font-size: 13px; line-height: 21px; color: #555;">
                            만약 버튼이 정상적으로 클릭되지 않는다면, 아래 링크를 복사하여 접속해 주세요.<br />
                            %s
                        </p>
                    </div>
                </div>
                """.formatted(authUrl, authUrl);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("[펫로그] 이메일 인증");
            helper.setText(htmlContent, true); // true: HTML 형식

            mailSender.send(message);
            System.out.printf("메일 전송 완료");
        } catch (MessagingException e) {
            // 예외 처리 로직
            e.printStackTrace();
        }
    }

    @Transactional(readOnly = false)
    public void verify(String uuid, HttpServletResponse response) {
        System.out.println(uuid);
        EmailVerify emailVerify = emailVerifyRepository.findByUuid(uuid).orElseThrow(
                () -> new BaseException(BaseResponseStatus.INVALID_USER_INFO));

        User user = emailVerify.getUser();
        user.userVerify();

        userRepository.save(user);
        try {
            response.sendRedirect(frontendServer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional(readOnly = false)
    public UserDto.CreateResponse signup(UserDto.CreateRequest dto) {
        // 이메일 중복 확인
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        String uuid = UUID.randomUUID().toString();

        String resolvedProfileImage = (dto.getProfileImageUrl() == null || dto.getProfileImageUrl().isBlank())
                ? defaultProfileImageUrl
                : dto.getProfileImageUrl();

        User user = dto.toEntity(passwordEncoder.encode(dto.getPassword()), resolvedProfileImage);
        userRepository.save(user);


        if (!user.getEnabled()) {
            emailVerifyRepository.save(EmailVerify.builder()
                    .user(user)
                    .uuid(uuid)
                    .build());
            sendEmail(uuid, dto.getEmail());
        }
        return UserDto.CreateResponse.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            Optional<User> result = userRepository.findByEmail(email);

            if (result.isPresent()) {
                User user = result.get();

                if (user.getIsDeleted()) {
                    throw new InternalAuthenticationServiceException(
                            BaseResponseStatus.USER_DELETED_LOGIN.getMessage(),
                            new BaseException(BaseResponseStatus.USER_DELETED_LOGIN)
                    );
                }

                return user;
            }

            return null;

        } catch (BaseException ex) {
            throw new RuntimeException("알 수 없는 로그인 오류", ex);
        }
    }

    @Transactional(readOnly = true)
    public UserDto.LoginCheckResponse checkLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null &&
                authentication.isAuthenticated() &&
                !(authentication instanceof AnonymousAuthenticationToken)) {

            User user = (User) authentication.getPrincipal();

            return UserDto.LoginCheckResponse.from(user);

        }

        return UserDto.LoginCheckResponse.builder()
                .idx(0L)
                .isLogin(false)
                .enabled(false)
                .nickname(null)
                .build();
    }

    public void logout(HttpServletResponse response) {
        ResponseCookie deleteCookie = ResponseCookie.from("ATOKEN", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .build();

        response.setHeader("Set-Cookie", deleteCookie.toString());
    }

    @Transactional(readOnly = true)
    public User findUserByIdx(Long idx) {
        return userRepository.findByIdx(idx).orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));
    }

    // 유저 프로필 조회
    @Transactional(readOnly = true)
    public UserDto.UserProfileResponse getProfile(Long idx) {
        User user = userRepository.findById(idx)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // 펫 카드 목록 (현재는 비어있지만 추후 구현 가능)
        List<PetDto.PetCard> petCards = new ArrayList<>();

        System.out.println("Returning user email: " + user.getEmail());
        // DB에서 가져온 정보를 UserProfileResponse로 반환
        return UserDto.UserProfileResponse.builder()
                .provider(user.getProvider())
                .email(user.getEmail())  // DB에서 가져온 이메일
                .nickname(user.getNickname())  // DB에서 가져온 닉네임
                .profileImageUrl(user.getUserProfileImage())  // DB에서 가져온 프로필 이미지 URL
                .petCards(petCards)  // 펫 카드 목록
                .build();
    }

    @Transactional(readOnly = false)
    public void updateProfileImage(Long userId, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));
        user.setUserProfileImage(imageUrl);
        userRepository.save(user);
    }

    @Transactional(readOnly = false)
    public void updatePassword(Long userId, String currentPassword, String newPassword) {
        // 사용자 찾기
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }

        if (!newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$")) {
            throw new IllegalArgumentException("새 비밀번호는 영문, 숫자, 특수문자를 포함해 8자 이상이어야 합니다.");
        }

        String encodedNewPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedNewPassword);
        userRepository.save(user);
    }

    // 닉네임 수정
    @Transactional(readOnly = false)
    public void updateNickname(Long userId, String newNickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 닉네임 길이 체크 (4글자 이상)
        if (newNickname == null || newNickname.length() < 4) {
            throw new RuntimeException("닉네임은 4글자 이상이어야 합니다.");
        }


        // 닉네임 중복 체크 (원하는 경우)
        if (userRepository.existsByNickname(newNickname)) {
            throw new RuntimeException("이미 존재하는 닉네임입니다.");
        }

        user.setNickname(newNickname);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Boolean checkEmailDuplicate(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.isPresent();
    }

    @Transactional(readOnly = false)
    public void deleteUser(Long idx, UserDto.UserDeleteRequest request) {
        User user = userRepository.findByIdxAndIsDeletedFalse(idx)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));


        if (user.getProvider() == null || user.getProvider().isEmpty()) {
            // 일반 회원이라면 비밀번호 확인
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
            }
        }

        // Soft Delete 처리
        user.setIsDeleted(true);
    }

    // 삭제된 사용자 목록을 반환하는 메서드
    @Transactional(readOnly = true)
    public List<User> getDeletedUsers() {
        return userRepository.findByIsDeletedTrue();  // 'is_deleted'가 true인 사용자 목록 반환
    }

//    @Value("${kakao-admin-key}")
//    private String adminKey;

    public Boolean checkOAuthUser(User user) {
        User checkUser = userRepository.findById(user.getIdx()).orElseThrow(
                () -> new BaseException(BaseResponseStatus.USER_NOT_FOUND));

        return "kakao".equals(checkUser.getProvider());
    }


    public void refreshAccessToken(User user, HttpServletResponse response) {
        String email = user.getEmail();
//        String savedRefreshToken = refreshTokenRepository.findById(email);

//        if (savedRefreshToken == null) {
//            throw new RuntimeException("리프레시 토큰이 없습니다.");
//        }
//
//        if (!JwtUtil.validate(savedRefreshToken)) {
//            throw new RuntimeException("리프레시 토큰이 만료되었거나 잘못되었습니다.");
//        }
//
//        User refreshUser = JwtUtil.getUser(savedRefreshToken);
//        if (refreshUser == null || !refreshUser.getEmail().equals(email)) {
//            throw new RuntimeException("리프레시 토큰 정보가 일치하지 않습니다.");
//        }

        String newAccessToken = JwtUtil.generateToken(user);

        long refreshTokenExp = Duration.ofDays(7).toMillis();
        String newRefreshToken = JwtUtil.generateRefreshToken(user, refreshTokenExp);

//        refreshTokenRepository.save(email, newRefreshToken, refreshTokenExp);

        ResponseCookie accessCookie = ResponseCookie.from("ATOKEN", newAccessToken)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .maxAge(Duration.ofHours(1))
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
    }

}
