package com.example.olineaqspring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.olineaqspring.entity.SysUser;
import com.example.olineaqspring.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public List<SysUser> list() {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .orderByAsc(SysUser::getUserId));
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    public SysUser create(String username, String password, String realName, String role) {
        SysUser exist = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (exist != null) {
            throw new RuntimeException("账号「" + username + "」已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setRole(role);
        userMapper.insert(user);
        user.setPassword(null);
        return user;
    }

    public SysUser update(Integer userId, String realName, String password) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (realName != null && !realName.isEmpty()) {
            user.setRealName(realName);
        }
        if (password != null && !password.isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    public void delete(Integer userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        userMapper.deleteById(userId);
    }
}
