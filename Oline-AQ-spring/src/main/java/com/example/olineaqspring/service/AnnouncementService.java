package com.example.olineaqspring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.olineaqspring.dto.AnnouncementRequest;
import com.example.olineaqspring.entity.Announcement;
import com.example.olineaqspring.entity.AnnouncementRead;
import com.example.olineaqspring.mapper.AnnouncementMapper;
import com.example.olineaqspring.mapper.AnnouncementReadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementMapper announcementMapper;
    private final AnnouncementReadMapper announcementReadMapper;
    private final NotificationService notificationService;

    @Cacheable(value = "announcements", key = "'all'")
    public List<Announcement> listAll() {
        return announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>().orderByDesc(Announcement::getCreateTime));
    }

    @Cacheable(value = "announcements", key = "'active'")
    public List<Announcement> listActive() {
        return announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getActive, true)
                        .orderByDesc(Announcement::getCreateTime));
    }

    @Cacheable(value = "announcements", key = "#id")
    public Announcement getById(Integer id) {
        Announcement ann = announcementMapper.selectById(id);
        if (ann == null) throw new RuntimeException("公告不存在");
        return ann;
    }

    @Transactional
    @CacheEvict(value = "announcements", allEntries = true)
    public Announcement create(AnnouncementRequest request, Integer operatorId) {
        Announcement ann = new Announcement();
        ann.setTitle(request.getTitle());
        ann.setContent(request.getContent());
        ann.setActive(request.getActive() != null ? request.getActive() : true);
        ann.setCreateTime(LocalDateTime.now());
        ann.setUpdateTime(LocalDateTime.now());
        announcementMapper.insert(ann);
        if (Boolean.TRUE.equals(ann.getActive())) {
            notificationService.notifyAnnouncement(ann.getAnnouncementId(), ann.getTitle());
        }
        return ann;
    }

    @Transactional
    @CacheEvict(value = "announcements", allEntries = true)
    public Announcement update(Integer id, AnnouncementRequest request) {
        Announcement ann = getById(id);
        ann.setTitle(request.getTitle());
        ann.setContent(request.getContent());
        if (request.getActive() != null) {
            ann.setActive(request.getActive());
        }
        ann.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(ann);
        return ann;
    }

    @Transactional
    @CacheEvict(value = "announcements", allEntries = true)
    public void delete(Integer id) {
        getById(id);
        announcementMapper.deleteById(id);
        announcementReadMapper.delete(new LambdaQueryWrapper<AnnouncementRead>()
                .eq(AnnouncementRead::getAnnouncementId, id));
    }

    public Map<String, Object> getUnreadInfo(Integer userId) {
        List<Announcement> active = listActive();
        if (active.isEmpty()) {
            return Map.of("unreadCount", 0, "announcements", List.of());
        }

        List<Integer> activeIds = active.stream().map(Announcement::getAnnouncementId).toList();
        List<AnnouncementRead> reads = announcementReadMapper.selectList(
                new LambdaQueryWrapper<AnnouncementRead>()
                        .eq(AnnouncementRead::getUserId, userId)
                        .in(AnnouncementRead::getAnnouncementId, activeIds));

        List<Map<String, Object>> result = active.stream().map(a -> {
            boolean read = reads.stream().anyMatch(r -> r.getAnnouncementId().equals(a.getAnnouncementId()));
            Map<String, Object> item = new HashMap<>();
            item.put("announcementId", a.getAnnouncementId());
            item.put("title", a.getTitle());
            item.put("content", a.getContent());
            item.put("createTime", a.getCreateTime());
            item.put("read", read);
            return item;
        }).toList();

        long unreadCount = result.stream().filter(item -> !(Boolean) item.get("read")).count();
        return Map.of("unreadCount", unreadCount, "announcements", result);
    }

    @Transactional
    public void markAsRead(Integer announcementId, Integer userId) {
        long exists = announcementReadMapper.selectCount(
                new LambdaQueryWrapper<AnnouncementRead>()
                        .eq(AnnouncementRead::getAnnouncementId, announcementId)
                        .eq(AnnouncementRead::getUserId, userId));
        if (exists == 0) {
            AnnouncementRead ar = new AnnouncementRead();
            ar.setAnnouncementId(announcementId);
            ar.setUserId(userId);
            ar.setReadTime(LocalDateTime.now());
            announcementReadMapper.insert(ar);
        }
    }

    @Transactional
    public void markAllAsRead(Integer userId) {
        List<Announcement> active = listActive();
        for (Announcement a : active) {
            markAsRead(a.getAnnouncementId(), userId);
        }
    }
}
