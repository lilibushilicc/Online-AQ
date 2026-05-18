package com.example.olineaqspring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.olineaqspring.dto.LoginRequest;
import com.example.olineaqspring.entity.SysUser;
import com.example.olineaqspring.exception.UnauthorizedException;
import com.example.olineaqspring.mapper.UserMapper;
import com.example.olineaqspring.utils.JwtUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    public AuthService(UserMapper userMapper, JwtUtils jwtUtils) {
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
    }

    public Map<String, Object> login(LoginRequest request) {
        // 根据用户名查询用户
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));

        // 先判断用户是否存在
        if (user == null) {
            throw new UnauthorizedException("账号或密码不正确");
        }

        // 再判断密码是否正确
        if (!user.getPassword().equals(request.getPassword())) {
            throw new UnauthorizedException("账号或密码不正确");
        }

        // 最后判断身份（角色）是否匹配
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
