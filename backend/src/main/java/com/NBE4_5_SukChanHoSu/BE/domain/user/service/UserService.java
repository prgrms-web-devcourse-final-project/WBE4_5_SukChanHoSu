package com.NBE4_5_SukChanHoSu.BE.domain.user.service;

import com.NBE4_5_SukChanHoSu.BE.domain.admin.service.AdminMonitoringService;
import com.NBE4_5_SukChanHoSu.BE.domain.email.service.EmailService;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.PasswordModifyRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.PasswordUpdateRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.UserLoginRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.UserSignUpRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.LoginResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.Role;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.User;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.UserStatus;
import com.NBE4_5_SukChanHoSu.BE.domain.user.repository.UserRepository;
import com.NBE4_5_SukChanHoSu.BE.domain.user.responseCode.UserErrorCode;
import com.NBE4_5_SukChanHoSu.BE.global.exception.ServiceException;
import com.NBE4_5_SukChanHoSu.BE.global.jwt.service.TokenService;
import com.NBE4_5_SukChanHoSu.BE.global.util.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private static final String EMAIL_VERIFY = "emailVerify:";
    private static final String TRUE = "true";

    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final AdminMonitoringService adminMonitoringService;

    public User join(UserSignUpRequest requestDto) {
        String verified = redisTemplate.opsForValue().get(EMAIL_VERIFY + requestDto.getEmail());

        if (!TRUE.equals(verified)) {
            throw new ServiceException(
                    UserErrorCode.EMAIL_NOT_VERIFY.getCode(),
                    UserErrorCode.EMAIL_NOT_VERIFY.getMessage()
            );
        }

        if (!requestDto.getPassword().equals(requestDto.getPasswordConfirm())) {
            throw new ServiceException(
                    UserErrorCode.PASSWORDS_NOT_MATCH.getCode(),
                    UserErrorCode.PASSWORDS_NOT_MATCH.getMessage()
            );
        }
        User checkUser = userRepository.findByEmail(requestDto.getEmail());

        if (checkUser != null) {
            throw new ServiceException(
                    UserErrorCode.EMAIL_ALREADY_EXISTS.getCode(),
                    UserErrorCode.EMAIL_ALREADY_EXISTS.getMessage()
            );
        }

        User user = User.builder()
                .email(requestDto.getEmail())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .role(Role.USER)
                .emailVerified(true)
                .status(UserStatus.ACTIVE)
                .build();
        // 사용자 가입 성공 후 총 가입자 수 증가
        adminMonitoringService.incrementTotalUsers();
        return userRepository.save(user);
    }

    public LoginResponse login(UserLoginRequest requestDto) {
        User user = userRepository.findByEmail(requestDto.getEmail());

        if (user == null) {
            throw new ServiceException(
                    UserErrorCode.EMAIL_NOT_FOUND.getCode(),
                    UserErrorCode.EMAIL_NOT_FOUND.getMessage()
            );
        }

        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new ServiceException(
                    UserErrorCode.PASSWORDS_NOT_MATCH.getCode(),
                    UserErrorCode.PASSWORDS_NOT_MATCH.getMessage()
            );
        }

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(requestDto.getEmail(), requestDto.getPassword());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        return tokenService.generateToken(authentication);
    }

    public void logout(String refreshToken) {
        long expirationTime = tokenService.getExpirationTimeFromToken(refreshToken);
        tokenService.addToBlacklist(refreshToken, expirationTime);
    }

    public void deleteUser() {
        User user = SecurityUtil.getCurrentUser();
        userRepository.delete(user);
    }

    @Transactional
    public void passwordUpdate(String refreshToken, PasswordUpdateRequest requestDto) {
        User currentUser = SecurityUtil.getCurrentUser();
        User user = userRepository.findByEmail(currentUser.getEmail());

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(requestDto.getCurrentPassword(), user.getPassword())) {
            throw new ServiceException(
                    UserErrorCode.PASSWORDS_NOT_MATCH.getCode(),
                    UserErrorCode.PASSWORDS_NOT_MATCH.getMessage()
            );
        }

        // 새 비밀번호와 확인 값 일치 확인
        if (!requestDto.getNewPassword().equals(requestDto.getConfirmNewPassword())) {
            throw new ServiceException(
                    UserErrorCode.NEW_PASSWORDS_NOT_MATCH.getCode(),
                    UserErrorCode.NEW_PASSWORDS_NOT_MATCH.getMessage()
            );
        }

        // 비밀번호 업데이트
        user.setPassword(passwordEncoder.encode(requestDto.getNewPassword()));

        logout(refreshToken);
    }

    @Transactional
    public void passwordModify(PasswordModifyRequest requestDto) {
        User user = userRepository.findByEmail(requestDto.getEmail());

        // 새 비밀번호와 확인 값 일치 확인
        if (!requestDto.getNewPassword().equals(requestDto.getConfirmNewPassword())) {
            throw new ServiceException(
                    UserErrorCode.NEW_PASSWORDS_NOT_MATCH.getCode(),
                    UserErrorCode.NEW_PASSWORDS_NOT_MATCH.getMessage()
            );
        }

        user.setPassword(passwordEncoder.encode(requestDto.getNewPassword()));
    }
}
