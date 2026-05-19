package com.example.olineaqspring.controller;

import com.example.olineaqspring.entity.WrongNotebook;
import com.example.olineaqspring.service.WrongNotebookService;
import com.example.olineaqspring.vo.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wrong-notebooks")
public class WrongNotebookController {
    private final WrongNotebookService wrongNotebookService;

    public WrongNotebookController(WrongNotebookService wrongNotebookService) {
        this.wrongNotebookService = wrongNotebookService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listNotebooks(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return ApiResponse.ok("查询成功", wrongNotebookService.getNotebookItemCounts(userId));
    }

    @PostMapping
    public ApiResponse<WrongNotebook> createNotebook(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Integer userId = (Integer) request.getAttribute("userId");
        String name = body.get("notebookName");
        if (name == null || name.isBlank()) {
            return ApiResponse.fail("错题本名称不能为空");
        }
        return ApiResponse.ok("创建成功", wrongNotebookService.createNotebook(userId, name, body.get("description")));
    }

    @PutMapping("/{notebookId}")
    public ApiResponse<WrongNotebook> updateNotebook(@PathVariable Integer notebookId, HttpServletRequest request,
                                                      @RequestBody Map<String, String> body) {
        Integer userId = (Integer) request.getAttribute("userId");
        return ApiResponse.ok("更新成功",
                wrongNotebookService.updateNotebook(notebookId, userId, body.get("notebookName"), body.get("description")));
    }

    @DeleteMapping("/{notebookId}")
    public ApiResponse<Void> deleteNotebook(@PathVariable Integer notebookId, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        wrongNotebookService.deleteNotebook(notebookId, userId);
        return ApiResponse.ok("删除成功", null);
    }

    @GetMapping("/{notebookId}")
    public ApiResponse<Map<String, Object>> getNotebookDetail(@PathVariable Integer notebookId,
                                                               HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return ApiResponse.ok("查询成功", wrongNotebookService.getNotebookDetail(notebookId, userId));
    }

    @PostMapping("/{notebookId}/items")
    public ApiResponse<Map<String, Object>> addItem(@PathVariable Integer notebookId, HttpServletRequest request,
                                                     @RequestBody Map<String, Integer> body) {
        Integer userId = (Integer) request.getAttribute("userId");
        Integer answerId = body.get("answerId");
        if (answerId == null) {
            return ApiResponse.fail("答题记录ID不能为空");
        }
        return ApiResponse.ok("添加成功", wrongNotebookService.addItem(notebookId, userId, answerId));
    }

    @DeleteMapping("/{notebookId}/items/{itemId}")
    public ApiResponse<Void> removeItem(@PathVariable Integer notebookId, @PathVariable Integer itemId,
                                         HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        wrongNotebookService.removeItem(notebookId, itemId, userId);
        return ApiResponse.ok("移除成功", null);
    }
}
