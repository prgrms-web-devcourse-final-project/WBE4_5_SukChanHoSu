package com.NBE4_5_SukChanHoSu.BE.domain.user.responseCode;

import lombok.Getter;

@Getter
public enum UserSuccessCode {
    JOIN_SUCCESS("200-1", "회원가입이 완료되었습니다."),
    LOGIN_SUCCESS("200-2", "로그인이 완료되었습니다."),
    LOGOUT_SUCCESS("200-3", "로그아웃이 완료되었습니다."),
    WITHDREW_SUCCESS("200-4", "회원탈퇴가 완료되었습니다."),
    PROFILE_FETCH_SUCCESS("200-5", "프로필 조회 성공"),
    REISSUE_SUCCESS("200-6", "accessToken 재발급 성공"),
    PASSWORD_UPDATE_SUCCESS("200-7", "비밀번호 변경 성공");

    private final String code;
    private final String message;

    UserSuccessCode(String code, String message) {
        this.code = code.split("-")[0];
        this.message = message;
    }
}
