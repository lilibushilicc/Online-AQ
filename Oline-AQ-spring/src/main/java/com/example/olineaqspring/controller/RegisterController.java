package com.example.olineaqspring.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.olineaqspring.dto.RegisterRequest;
import com.example.olineaqspring.dto.SendCodeRequest;
import com.example.olineaqspring.entity.SysUser;
import com.example.olineaqspring.mapper.UserMapper;
import com.example.olineaqspring.service.ConfigService;
import com.example.olineaqspring.service.EmailService;
import com.example.olineaqspring.utils.JwtUtils;
import com.example.olineaqspring.vo.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegisterController {

    private final EmailService emailService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final ConfigService configService;

    @PostMapping("/register/send-code")
    public ApiResponse<Void> sendCode(@RequestBody @Valid SendCodeRequest request) {
        validateEmailRegistration(request.getEmail());

        try {
            emailService.getActiveAccount();
        } catch (RuntimeException e) {
            return ApiResponse.fail(e.getMessage());
        }

        String code = emailService.generateCode();
        emailService.saveCode(request.getEmail(), code);
        emailService.sendVerificationCode(request.getEmail(), code);
        return ApiResponse.ok("验证码已发送到您的邮箱", null);
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody @Valid RegisterRequest request) {
        validateEmailRegistration(request.getEmail());

        boolean verified = emailService.verifyCode(request.getEmail(), request.getCode());
        if (!verified) {
            return ApiResponse.fail("验证码无效或已过期");
        }

        String base = request.getEmail().substring(0, request.getEmail().indexOf('@'));
        String username = base;
        int suffix = 1;
        while (userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0) {
            username = base + "_" + (++suffix);
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setRole("student");
        user.setEmail(request.getEmail());
        userMapper.insert(user);

        Map<String, Object> data = new HashMap<>();
        data.put("token", jwtUtils.generateToken(user));
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("role", user.getRole());
        return ApiResponse.ok("注册成功", data);
    }

    private void validateEmailRegistration(String email) {
        if (!"true".equals(configService.get("register.email.enabled"))) {
            throw new RuntimeException("邮箱注册功能未开启，请联系管理员");
        }
        LambdaQueryWrapper<SysUser> userQuery = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, email);
        if (userMapper.selectCount(userQuery) > 0) {
            throw new RuntimeException("该邮箱已被注册");
        }
    }
}
