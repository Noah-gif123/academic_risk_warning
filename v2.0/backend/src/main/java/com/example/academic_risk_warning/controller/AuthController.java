package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.ChangePasswordRequest;
import com.example.academic_risk_warning.dto.LoginRequest;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.AuthService;
import com.example.academic_risk_warning.service.CaptchaService;
import com.example.academic_risk_warning.vo.CaptchaVO;
import com.example.academic_risk_warning.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/** 统一登录接口：四端共用（含图形验证码） */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    /** 调试开关：默认关闭；仅本机联调时可临时打开，用于自动化脚本取验证码明文 */
    @Value("${app.auth.captcha-debug:false}")
    private boolean captchaDebug;

    /** 取一张新的图形验证码（无需登录） */
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.ok(captchaService.generate());
    }

    /** 调试：取回验证码明文（capter-debug=true 时才可用） */
    @GetMapping("/captcha/debug")
    public Result<String> captchaDebug(@RequestParam String captchaId) {
        if (!captchaDebug) {
            throw new BizException(404, "未开启验证码调试");
        }
        return Result.ok(captchaService.peek(captchaId));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        authService.logout(token);
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<LoginVO> me() {
        LoginUser user = UserContext.require();
        return Result.ok(authService.me(user));
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        LoginUser user = UserContext.require();
        authService.changePassword(user.getAccountId(), request);
        return Result.ok();
    }
}
