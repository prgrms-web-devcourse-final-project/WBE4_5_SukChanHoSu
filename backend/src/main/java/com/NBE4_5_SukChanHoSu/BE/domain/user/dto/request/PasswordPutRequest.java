package com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request;

import lombok.Data;

@Data
public class PasswordPutRequest {
    private String currentPassword;
    private String newPassword;
    private String confirmNewPassword;
}