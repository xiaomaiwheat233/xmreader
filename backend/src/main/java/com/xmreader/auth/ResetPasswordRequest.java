package com.xmreader.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "[A-Za-z0-9_]{3,32}", message = "用户名必须为 3 到 32 位字母、数字或下划线")
        String username,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 72, message = "新密码长度必须为 8 到 72")
        String newPassword,
        @NotBlank(message = "请再次输入新密码")
        @Size(min = 8, max = 72, message = "确认密码长度必须为 8 到 72")
        String confirmPassword) {

    @AssertTrue(message = "两次输入的密码不一致")
    public boolean isPasswordConfirmed() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
