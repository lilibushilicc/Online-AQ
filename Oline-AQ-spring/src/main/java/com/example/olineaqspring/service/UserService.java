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

    public SysUser getById(Integer userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        return user;
    }

    public SysUser update(Integer userId, String realName, String email, String password) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if (realName != null && !realName.isEmpty()) user.setRealName(realName);
        if (email != null && !email.isEmpty()) user.setEmail(email);
        if (password != null && !password.isEmpty()) user.setPassword(passwordEncoder.encode(password));
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    public SysUser updateMyProfile(Integer userId, String realName, String email, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");

        if (newPassword != null && !newPassword.isEmpty()) {
            if (oldPassword == null || oldPassword.isEmpty()) {
                throw new RuntimeException("修改密码需要输入旧密码");
            }
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new RuntimeException("旧密码不正确");
            }
            if (newPassword.length() < 6) {
                throw new RuntimeException("新密码长度不能少于6位");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        if (realName != null && !realName.isEmpty()) user.setRealName(realName);
        if (email != null && !email.isEmpty()) {
            if (!email.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$")) {
                throw new RuntimeException("邮箱格式不正确");
            }
            user.setEmail(email);
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
