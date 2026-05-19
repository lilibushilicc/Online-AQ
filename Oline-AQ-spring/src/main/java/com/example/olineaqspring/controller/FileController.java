package com.example.olineaqspring.controller;

import com.example.olineaqspring.service.FileService;
import com.example.olineaqspring.vo.ApiResponse;
import com.example.olineaqspring.vo.UploadFileVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return ApiResponse.ok("上传成功", fileService.upload(file, userId));
    }

    @PostMapping("/{fileId}/parse")
    public ApiResponse<Map<String, Object>> parse(
            @PathVariable Integer fileId,
            @RequestParam(required = false) String category) {
        return ApiResponse.ok("解析成功", fileService.parse(fileId, category));
    }
}
