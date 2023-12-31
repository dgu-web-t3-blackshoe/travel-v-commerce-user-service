package com.travelvcommerce.userservice.controller;

import com.travelvcommerce.userservice.dto.EmailDto;
import com.travelvcommerce.userservice.dto.ResponseDto;
import com.travelvcommerce.userservice.dto.TokenDto;
import com.travelvcommerce.userservice.entity.User;
import com.travelvcommerce.userservice.repository.RefreshTokenRepository;
import com.travelvcommerce.userservice.repository.UserRepository;
import com.travelvcommerce.userservice.security.JwtTokenProvider;
import com.travelvcommerce.userservice.service.EmailService;
import com.travelvcommerce.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import javax.naming.AuthenticationException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/user-service")
@RequiredArgsConstructor
public class UserServiceController {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$");

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
//    private final SocialLoginService naverLoginService;
//    private final SocialLoginService kakaoLoginService;
    @PostMapping("/refresh")
    public ResponseEntity<ResponseDto> refreshToken(@RequestBody TokenDto.RefreshTokenDto refreshTokenDto) {
        try {
            String refreshToken = refreshTokenDto.getRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().body(ResponseDto.builder()
                        .error("리프레시 토큰은 비어 있거나 null일 수 없습니다.")
                        .build());
            }

            // 1. refreshToken으로부터 email과 userType을 추출한다.
            String emailFromToken = jwtTokenProvider.getEmailFromToken(refreshToken);
            String userTypeFromToken = jwtTokenProvider.getUserTypeFromToken(refreshToken);

            // 2. email과 userType으로 refreshToken이 있는지 찾는다.
            String existingRefreshToken = refreshTokenRepository.find(userTypeFromToken, emailFromToken);
            if (existingRefreshToken == null || existingRefreshToken.isEmpty()) {
                throw new BadCredentialsException("리프레시 토큰이 존재하지 않습니다.");
            }

            // 3. DB에서 찾은 refreshToken과 요청의 refreshToken이 일치하는지 검사한다.
            if (!existingRefreshToken.equals(refreshToken)) {
                throw new BadCredentialsException("리프레시 토큰이 일치하지 않습니다.");
            }

            // 4. refreshToken이 존재한다면 new Token을 발급한다.
            String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
            TokenDto tokenDto = jwtTokenProvider.createTokens(emailFromToken, userTypeFromToken, userId);
            return ResponseEntity.ok()
                    .body(ResponseDto.builder()
                            .payload(new HashMap<String, Object>() {{
                                put("refreshToken", tokenDto.getRefreshToken());
                                put("accessToken", tokenDto.getAccessToken());
                            }})
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDto.builder()
                    .error("토큰 갱신 오류: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseDto> logout(@RequestBody TokenDto.RefreshTokenDto refreshTokenDto) {
        try {
            String refreshToken = refreshTokenDto.getRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().body(ResponseDto.builder()
                        .error("리프레시 토큰은 비어 있거나 null일 수 없습니다.")
                        .build());
            }

            // 1. refreshToken으로부터 email과 userType을 추출한다.
            String emailFromToken = jwtTokenProvider.getEmailFromToken(refreshToken);
            String userTypeFromToken = jwtTokenProvider.getUserTypeFromToken(refreshToken);

            // 2. email과 userType으로 refreshToken이 있는지 찾는다.
            String existingRefreshToken = refreshTokenRepository.find(userTypeFromToken, emailFromToken);
            if (existingRefreshToken == null || existingRefreshToken.isEmpty()) {
                throw new BadCredentialsException("리프레시 토큰이 존재하지 않습니다.");
            }

            // 3. DB에서 찾은 refreshToken과 요청의 refreshToken이 일치하는지 검사한다.
            if (!existingRefreshToken.equals(refreshToken)) {
                throw new BadCredentialsException("리프레시 토큰이 일치하지 않습니다.");
            }

            // 4. refreshToken이 존재한다면 DB에서 삭제한다.
            refreshTokenRepository.deleteRefreshTokenByUserEmail(userTypeFromToken, emailFromToken);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDto.builder()
                    .error("로그아웃 오류: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/mail/send-verification-code")
    public ResponseEntity<ResponseDto> generateVerificationCode(@RequestBody EmailDto.EmailRequestDto emailRequestDto) {
        try {
            if (!EMAIL_PATTERN.matcher(emailRequestDto.getEmail()).matches()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDto.builder().error("이메일 형식이 올바르지 않습니다.").build());
            }

            String verificationCode = emailService.generateVerificationCode();

            // 이메일로 인증 코드 전송
            emailService.sendMail(emailRequestDto.getEmail(), "[Wanderlust] 인증 코드가 도착했습니다.", "인증코드 : " + verificationCode);
            //redis에 인증 코드 저장
            emailService.saveVerificationCode(emailRequestDto.getEmail(), verificationCode);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            ResponseDto responseDto = ResponseDto.builder().error(e.getMessage()).build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseDto);
        }
    }

    @PostMapping("mail/verify-code")
    public ResponseEntity<ResponseDto> verifyCode(@RequestBody EmailDto.EmailVerificationRequestDto emailVerificationDto) {
        try {
            if(emailService.checkVerificationCode(emailVerificationDto.getEmail(), emailVerificationDto.getVerificationCode())) {
                emailService.deleteVerificationCode(emailVerificationDto.getEmail());
                emailService.saveCompletionCode(emailVerificationDto.getEmail());
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDto.builder().error("인증 코드가 일치하지 않습니다.").build());
            }
        } catch (RuntimeException e) {
            ResponseDto responseDto = ResponseDto.builder().error(e.getMessage()).build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseDto);
        }
    }
}
