package com.example.olineaqspring.controller;

import com.example.olineaqspring.annotation.AdminOnly;
import com.example.olineaqspring.service.FileService;
import com.example.olineaqspring.vo.ApiResponse;
import com.example.olineaqspring.vo.UploadFileVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.olineaqspring.entity.Question;
import java.util.List;
import java.util.Map;

@AdminOnly("仅管理员可操作文件管理")
@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    public ApiResponse<List<UploadFileVO>> listFiles() {
        return ApiResponse.ok("查询成功", fileService.listFiles());
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(required = false) String name,
                                                   HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return ApiResponse.ok("上传成功", fileService.upload(file, userId, name));
    }

    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> deleteFile(@PathVariable Integer fileId) {
        fileService.deleteFile(fileId);
        return ApiResponse.ok("删除成功", null);
    }

    @PostMapping("/{fileId}/parse")
    public ApiResponse<Map<String, Object>> parse(
            @PathVariable Integer fileId,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean useAi,
            HttpServletRequest request) {
        return ApiResponse.ok("解析成功", fileService.parse(fileId, category, useAi));
    }

    @PostMapping("/{fileId}/preview")
    public ApiResponse<List<Question>> preview(
            @PathVariable Integer fileId,
            @RequestBody Map<String, Object> body) {
        String category = (String) body.get("category");
        boolean useAi = body.get("useAi") instanceof Boolean && (Boolean) body.get("useAi");
        return ApiResponse.ok("预览成功", fileService.preview(fileId, category, useAi));
    }

    @PostMapping("/{fileId}/import")
    public ApiResponse<Map<String, Object>> importQuestions(
            @PathVariable Integer fileId,
            @RequestBody Map<String, List<Question>> body) {
        List<Question> questions = body.get("questions");
        if (questions == null || questions.isEmpty()) {
            throw new RuntimeException("请选择要导入的题目");
        }
        return ApiResponse.ok("导入成功", fileService.importQuestions(fileId, questions));
    }
}
