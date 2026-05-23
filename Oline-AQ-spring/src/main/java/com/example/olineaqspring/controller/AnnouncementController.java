package com.example.olineaqspring.controller;

import com.example.olineaqspring.annotation.AdminOnly;
import com.example.olineaqspring.dto.AnnouncementRequest;
import com.example.olineaqspring.entity.Announcement;
import com.example.olineaqspring.service.AnnouncementService;
import com.example.olineaqspring.vo.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public ApiResponse<List<Announcement>> list() {
        return ApiResponse.ok(announcementService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Announcement> get(@PathVariable Integer id) {
        return ApiResponse.ok(announcementService.getById(id));
    }

    @PostMapping
    @AdminOnly("仅管理员可创建公告")
    public ApiResponse<Announcement> create(@RequestBody AnnouncementRequest request, HttpServletRequest httpRequest) {
        Integer userId = (Integer) httpRequest.getAttribute("userId");
        return ApiResponse.ok("创建成功", announcementService.create(request, userId));
    }

    @PutMapping("/{id}")
    @AdminOnly("仅管理员可修改公告")
    public ApiResponse<Announcement> update(@PathVariable Integer id, @RequestBody AnnouncementRequest request) {
        return ApiResponse.ok("更新成功", announcementService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @AdminOnly("仅管理员可删除公告")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        announcementService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }

    @GetMapping("/unread")
    public ApiResponse<Map<String, Object>> unreadInfo(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return ApiResponse.ok(announcementService.getUnreadInfo(userId));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Integer id, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        announcementService.markAsRead(id, userId);
        return ApiResponse.ok("已标记", null);
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        announcementService.markAllAsRead(userId);
        return ApiResponse.ok("已全部标记", null);
    }
}
