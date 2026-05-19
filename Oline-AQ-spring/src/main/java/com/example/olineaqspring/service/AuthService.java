package com.example.olineaqspring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.olineaqspring.dto.LoginRequest;
import com.example.olineaqspring.entity.SysUser;
import com.example.olineaqspring.exception.UnauthorizedException;
import com.example.olineaqspring.mapper.UserMapper;
import com.example.olineaqspring.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    public Map<String, Object> login(LoginRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));

        if (user == null) {
            throw new UnauthorizedException("账号或密码不正确");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("账号或密码不正确");
        }

        if (!user.getRole().equals(request.getRole())) {
            throw new UnauthorizedException("账号或密码不正确");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("token", jwtUtils.generateToken(user));
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("role", user.getRole());
        return data;
    }
}
