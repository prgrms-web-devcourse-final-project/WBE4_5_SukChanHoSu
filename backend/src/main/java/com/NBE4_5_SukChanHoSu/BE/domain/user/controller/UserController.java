package com.NBE4_5_SukChanHoSu.BE.domain.user.controller;

import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.PasswordPutRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.UserLoginRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.UserSignUpRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.LoginResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.UserResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.User;
import com.NBE4_5_SukChanHoSu.BE.domain.user.responseCode.UserErrorCode;
import com.NBE4_5_SukChanHoSu.BE.domain.user.responseCode.UserSuccessCode;
import com.NBE4_5_SukChanHoSu.BE.domain.user.service.UserService;
import com.NBE4_5_SukChanHoSu.BE.global.dto.RsData;
import com.NBE4_5_SukChanHoSu.BE.global.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "인증 및 사용자 관리", description = "로그인, 회원가입, 로그아웃, 내 프로필 조회 등 API")
public class UserController {
    private final UserService userService;
    private final CookieUtil cookieUtil;

    private static final String GOOGLE_AUTHORIZATION_PATH = "/oauth2/authorization/google";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @GetMapping("/google/url")
    @Operation(summary = "구글 로그인 URL 요청", description = "구글 OAuth2 로그인 페이지로 리다이렉트할 수 있는 URL 반환")
    public String getGoogleLoginUrl(HttpServletRequest request) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        return baseUrl + GOOGLE_AUTHORIZATION_PATH;
    }

    @PostMapping("/join")
    @Operation(
            summary = "회원가입",
            description = "사용자 회원가입 요청 처리",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "회원가입 성공"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (이메일 미인증, 비밀번호 불일치, 이메일 중복)"
                    )
            }
    )
    public RsData<UserResponse> join(@Valid @RequestBody UserSignUpRequest requestDto) {
        User user = userService.join(requestDto);

        return new RsData<>(
                UserSuccessCode.JOIN_SUCCESS.getCode(),
                UserSuccessCode.JOIN_SUCCESS.getMessage(),
                new UserResponse(user)
        );
    }

    @PostMapping("/login")
    @Operation(
            summary = "로그인",
            description = "사용자 로그인 후 AccessToken 과 RefreshToken 발급",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "로그인 성공 및 토큰 발급"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (존재하지 않는 이메일, 비밀번호 불일치)"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    public RsData<LoginResponse> login(
            @Valid @RequestBody UserLoginRequest requestDto,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        LoginResponse loginResponse = userService.login(requestDto);

        cookieUtil.addAccessCookie(loginResponse.getAccessToken(), request, response);
        cookieUtil.addRefreshCookie(loginResponse.getRefreshToken(), request, response);

        return new RsData<>(
                UserSuccessCode.LOGIN_SUCCESS.getCode(),
                UserSuccessCode.LOGIN_SUCCESS.getMessage(),
                loginResponse
        );
    }

    @PostMapping("/logout")
    @Operation(
            summary = "로그아웃",
            description = "AccessToken과 RefreshToken을 삭제하여 로그아웃 처리, RefreshToken 블랙리스트 처리",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "로그아웃 성공"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "유효하지 않은 토큰 또는 토큰 없음"
                    )
            }
    )
    public RsData<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        String accessToken = null;

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            accessToken = authHeader.substring(7);
        }

        if (accessToken == null) {
            accessToken = cookieUtil.getAccessTokenFromCookie(request);
        }

        String refreshToken = cookieUtil.getRefreshTokenFromCookie(request);

        if (accessToken == null || refreshToken == null) {
            return new RsData<>(
                    UserErrorCode.LOGOUT_UNAUTHORIZED.getCode(),
                    UserErrorCode.LOGOUT_UNAUTHORIZED.getMessage()
            );
        }

        userService.logout(refreshToken);

        cookieUtil.deleteAccessTokenFromCookie(response);
        cookieUtil.deleteRefreshTokenFromCookie(response);

        return new RsData<>(
                UserSuccessCode.LOGOUT_SUCCESS.getCode(),
                UserSuccessCode.LOGOUT_SUCCESS.getMessage()
        );
    }

    @PatchMapping
    @Operation(
            summary = "비밀번호 변경",
            description = "현재 비밀번호를 확인한 후, 새 비밀번호로 변경합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
                    @ApiResponse(responseCode = "400", description = "새 비밀번호와 확인 비밀번호가 일치하지 않음"),
                    @ApiResponse(responseCode = "400", description = "현재 비밀번호가 일치하지 않음")
            }
    )
    public RsData<?> userPasswordUpdate(@RequestBody PasswordPutRequest requestDto) {
        userService.passwordUpdate(requestDto);

        return new RsData<>(
                UserSuccessCode.PASSWORD_UPDATE_SUCCESS.getCode(),
                UserSuccessCode.PASSWORD_UPDATE_SUCCESS.getMessage()
        );
    }

    @DeleteMapping
    @Operation(
            summary = "회원탈퇴",
            description = "로그인한 사용자의 회원탈퇴 처리 및 인증 토큰 삭제",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "회원탈퇴 성공"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증되지 않은 사용자"
                    )
            }
    )
    public RsData<?> deleteUser(HttpServletResponse response) {
        userService.deleteUser();

        cookieUtil.deleteAccessTokenFromCookie(response);
        cookieUtil.deleteRefreshTokenFromCookie(response);

        return new RsData<>(
                UserSuccessCode.WITHDREW_SUCCESS.getCode(),
                UserSuccessCode.WITHDREW_SUCCESS.getMessage()
        );
    }
}
