package com.NBE4_5_SukChanHoSu.BE.domain.user.service;

import com.NBE4_5_SukChanHoSu.BE.domain.admin.service.AdminMonitoringService;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.UserLoginRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.UserSignUpRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.LoginResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.Role;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.User;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.UserStatus;
import com.NBE4_5_SukChanHoSu.BE.domain.user.repository.UserRepository;
import com.NBE4_5_SukChanHoSu.BE.domain.user.responseCode.UserErrorCode;
import com.NBE4_5_SukChanHoSu.BE.global.exception.ServiceException;
import com.NBE4_5_SukChanHoSu.BE.global.exception.user.UserNotFoundException;
import com.NBE4_5_SukChanHoSu.BE.global.jwt.service.TokenService;
import com.NBE4_5_SukChanHoSu.BE.global.util.SecurityUtil;
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

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                                UserErrorCode.USER_NOT_FOUND.getCode(),
                                UserErrorCode.USER_NOT_FOUND.getMessage()
                        )
                );
    }

    public void logout(String refreshToken) {
        long expirationTime = tokenService.getExpirationTimeFromToken(refreshToken);
        tokenService.addToBlacklist(refreshToken, expirationTime);
    }

    public void deleteUser() {
        User user = SecurityUtil.getCurrentUser();
        userRepository.delete(user);
    }
}
