package com.example.olineaqspring.service;

import com.example.olineaqspring.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {
    private final Map<Integer, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final JwtUtils jwtUtils;

    public NotificationService(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    public SseEmitter subscribe(String token) {
        Claims claims = jwtUtils.parseToken(token);
        Integer userId = claims.get("userId", Integer.class);

        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ignored) {}

        return emitter;
    }

    public void notifyExamPublished(Integer examId, String examName) {
        broadcast("exam_published", "新考试发布：" + examName, Map.of("examId", examId, "examName", examName));
    }

    public void notifyAnnouncement(Integer announcementId, String title) {
        broadcast("announcement", title, Map.of("announcementId", announcementId, "title", title));
    }

    private void broadcast(String eventName, String message, Map<String, Object> data) {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(Map.of("message", message, "data", data)));
            } catch (IOException e) {
                emitters.remove(userId);
            }
        });
    }
}
