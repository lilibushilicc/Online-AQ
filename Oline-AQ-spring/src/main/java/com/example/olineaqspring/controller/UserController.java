package com.example.olineaqspring.controller;

import com.example.olineaqspring.annotation.AdminOnly;
import com.example.olineaqspring.config.CurrentUser;
import com.example.olineaqspring.config.UserPrincipal;
import com.example.olineaqspring.entity.SysUser;
import com.example.olineaqspring.service.UserService;
import com.example.olineaqspring.vo.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @AdminOnly("仅管理员可查看用户列表")
    public ApiResponse<List<SysUser>> list() {
        return ApiResponse.ok("查询成功", userService.list());
    }

    @GetMapping("/me")
    public ApiResponse<SysUser> myProfile(@CurrentUser UserPrincipal user) {
        SysUser u = userService.getById(user.getUserId());
        u.setPassword(null);
        return ApiResponse.ok(u);
    }

    @GetMapping("/{userId}")
    @AdminOnly("仅管理员可查看用户详情")
    public ApiResponse<SysUser> getUser(@PathVariable Integer userId) {
        SysUser u = userService.getById(userId);
        u.setPassword(null);
        return ApiResponse.ok(u);
    }

    @PutMapping("/me")
    public ApiResponse<SysUser> updateMyProfile(@CurrentUser UserPrincipal user,
                                                  @RequestBody Map<String, String> body) {
        SysUser u = userService.updateMyProfile(user.getUserId(),
                body.get("realName"), body.get("email"),
                body.get("oldPassword"), body.get("newPassword"));
        return ApiResponse.ok("修改成功", u);
    }

    @PostMapping
    @AdminOnly("仅管理员可新增用户")
    public ApiResponse<SysUser> create(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String realName = body.get("realName");
        String role = body.getOrDefault("role", "student");

        if (username == null || username.isEmpty()) return ApiResponse.fail("账号不能为空");
        if (password == null || password.isEmpty()) return ApiResponse.fail("密码不能为空");
        if (realName == null || realName.isEmpty()) return ApiResponse.fail("姓名不能为空");

        return ApiResponse.ok("新增成功", userService.create(username, password, realName, role));
    }

    @PutMapping("/{userId}")
    @AdminOnly("仅管理员可修改用户")
    public ApiResponse<SysUser> update(@PathVariable Integer userId, @RequestBody Map<String, String> body) {
        return ApiResponse.ok("修改成功",
                userService.update(userId, body.get("realName"), body.get("email"), body.get("password")));
    }

    @DeleteMapping("/{userId}")
    @AdminOnly("仅管理员可删除用户")
    public ApiResponse<Void> delete(@PathVariable Integer userId) {
        userService.delete(userId);
        return ApiResponse.ok("删除成功", null);
    }
}
