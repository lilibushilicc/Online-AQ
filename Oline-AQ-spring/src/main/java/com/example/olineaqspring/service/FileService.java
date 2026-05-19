package com.example.olineaqspring.service;

import com.example.olineaqspring.entity.Question;
import com.example.olineaqspring.entity.UploadFile;
import com.example.olineaqspring.mapper.QuestionMapper;
import com.example.olineaqspring.mapper.UploadFileMapper;
import com.example.olineaqspring.vo.UploadFileVO;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService {
    private final UploadFileMapper uploadFileMapper;
    private final QuestionMapper questionMapper;
    private final QuestionParseService questionParseService;
    private final AiQuestionParseService aiQuestionParseService;
    private final R2StorageService r2StorageService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public Map<String, Object> upload(MultipartFile file, Integer userId) {
        if (file.isEmpty()) throw new RuntimeException("上传文件不能为空");
        String originalName = file.getOriginalFilename() == null ? "unknown.txt" : file.getOriginalFilename();
        String suffix = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        if (!suffix.equals("txt") && !suffix.equals("docx")) {
            throw new RuntimeException("初版只支持 txt 和 docx 文件");
        }
        try {
            byte[] bytes = file.getBytes();
            String rawText = readRawText(bytes, suffix);

            String filePath;
            if (r2StorageService.isEnabled()) {
                String r2Key = UUID.randomUUID() + "-" + originalName;
                String contentType = suffix.equals("docx") ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document" : "text/plain";
                r2StorageService.upload(r2Key, bytes, contentType);
                filePath = "r2://" + r2Key;
            } else {
                Path dir = Path.of(uploadDir);
                Files.createDirectories(dir);
                Path target = dir.resolve(UUID.randomUUID() + "-" + originalName);
                Files.write(target, bytes);
                filePath = target.toString();
            }

            UploadFile uploadFile = new UploadFile();
            uploadFile.setFileName(originalName);
            uploadFile.setFileType(suffix);
            uploadFile.setFilePath(filePath);
            uploadFile.setRawText(rawText);
            uploadFile.setUploadUserId(userId);
            uploadFile.setStatus("uploaded");
            uploadFile.setCreateTime(LocalDateTime.now());
            uploadFileMapper.insert(uploadFile);

            Map<String, Object> data = new HashMap<>();
            data.put("fileId", uploadFile.getFileId());
            data.put("fileName", uploadFile.getFileName());
            data.put("status", uploadFile.getStatus());
            return data;
        } catch (Exception exception) {
            throw new RuntimeException("文件上传失败：" + exception.getMessage());
        }
    }

    public Map<String, Object> parse(Integer fileId) {
        return parse(fileId, null, false);
    }

    public Map<String, Object> parse(Integer fileId, String category, boolean useAi) {
        UploadFile uploadFile = uploadFileMapper.selectById(fileId);
        if (uploadFile == null) throw new RuntimeException("文件不存在");

        List<Question> questions;
        if (useAi) {
            questions = aiQuestionParseService.parse(uploadFile.getRawText(), fileId, category);
        } else {
            questions = questionParseService.parse(uploadFile.getRawText(), fileId, category);
        }

        questions.forEach(questionMapper::insert);
        uploadFile.setStatus("parsed");
        uploadFileMapper.updateById(uploadFile);
        Map<String, Object> data = new HashMap<>();
        data.put("fileId", fileId);
        data.put("questionCount", questions.size());
        return data;
    }

    public void deleteFile(Integer fileId) {
        UploadFile uploadFile = uploadFileMapper.selectById(fileId);
        if (uploadFile == null) throw new RuntimeException("文件不存在");

        questionMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Question>()
                .eq(Question::getSourceFileId, fileId));

        String filePath = uploadFile.getFilePath();
        if (filePath != null) {
            if (filePath.startsWith("r2://")) {
                String r2Key = filePath.substring(5);
                r2StorageService.delete(r2Key);
            } else {
                try {
                    Files.deleteIfExists(Path.of(filePath));
                } catch (Exception ignored) {}
            }
        }

        uploadFileMapper.deleteById(fileId);
    }

    public List<UploadFileVO> listFiles() {
        List<UploadFile> files = uploadFileMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UploadFile>()
                        .eq(UploadFile::getStatus, "parsed")
                        .orderByDesc(UploadFile::getCreateTime));
        if (files.isEmpty()) {
            return List.of();
        }
        List<Integer> fileIds = files.stream().map(UploadFile::getFileId).toList();
        Map<Integer, Long> countMap = questionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Question>()
                        .in(Question::getSourceFileId, fileIds)
        ).stream().collect(Collectors.groupingBy(Question::getSourceFileId, Collectors.counting()));

        List<UploadFileVO> result = new ArrayList<>();
        for (UploadFile f : files) {
            UploadFileVO vo = new UploadFileVO();
            vo.setFileId(f.getFileId());
            vo.setFileName(f.getFileName());
            vo.setFileType(f.getFileType());
            vo.setStatus(f.getStatus());
            vo.setQuestionCount(countMap.getOrDefault(f.getFileId(), 0L).intValue());
            vo.setCreateTime(f.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    private String readRawText(byte[] bytes, String suffix) throws Exception {
        if (suffix.equals("txt")) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            StringBuilder builder = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                builder.append(paragraph.getText()).append('\n');
            }
            return builder.toString();
        }
    }
}
