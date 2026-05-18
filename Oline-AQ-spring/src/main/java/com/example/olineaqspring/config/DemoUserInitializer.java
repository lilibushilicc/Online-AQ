package com.example.olineaqspring.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.olineaqspring.entity.SysUser;
import com.example.olineaqspring.mapper.UserMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoUserInitializer implements CommandLineRunner {
    private final UserMapper userMapper;

    public DemoUserInitializer(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void run(String... args) {
        ensureUser("admin", "123456", "教师管理员", "admin");
        ensureUser("2023001", "123456", "学生一号", "student");
    }

    private void ensureUser(String username, String password, String realName, String role) {
        SysUser exists = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (exists != null) {
            return;
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setRealName(realName);
        user.setRole(role);
        userMapper.insert(user);
    }
}
