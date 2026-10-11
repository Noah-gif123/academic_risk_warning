// src/main/java/com/example/redchina/dto/RegisterDto.java
package com.example.redchina.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class RegisterDto {
    @NotBlank(message = "{register.usernameRequired}")
    private String username;

    @NotBlank(message = "{register.passwordRequired}")
    @Size(min = 6, message = "密码长度不能少于6位")
    private String password;

    @NotBlank(message = "{register.confirmPasswordRequired}")
    private String confirmPassword;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;
}
