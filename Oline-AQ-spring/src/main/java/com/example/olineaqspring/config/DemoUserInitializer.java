package com.example.olineaqspring.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.olineaqspring.entity.SysUser;
import com.example.olineaqspring.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DemoUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserInitializer.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public DemoUserInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        migratePlaintextPasswords();
        ensureUser("admin", "123456", "教师管理员", "admin");
        ensureUser("2023001", "123456", "学生一号", "student");
    }

    private void migratePlaintextPasswords() {
        List<SysUser> users = userMapper.selectList(null);
        for (SysUser user : users) {
            String pw = user.getPassword();
            if (pw != null && !pw.startsWith("$2a$") && !pw.startsWith("$2b$")) {
                user.setPassword(passwordEncoder.encode(pw));
                userMapper.updateById(user);
                log.info("Migrated plaintext password for user: {}", user.getUsername());
            }
        }
    }

    private void ensureUser(String username, String password, String realName, String role) {
        SysUser exists = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (exists != null) {
            return;
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setRole(role);
        userMapper.insert(user);
    }
}
