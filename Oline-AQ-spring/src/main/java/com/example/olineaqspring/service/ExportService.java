package com.example.olineaqspring.service;

import com.example.olineaqspring.entity.ExamResult;
import com.example.olineaqspring.entity.Question;
import com.example.olineaqspring.entity.SysUser;
import com.example.olineaqspring.mapper.ExamMapper;
import com.example.olineaqspring.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final UserMapper userMapper;
    private final ExamMapper examMapper;

    public byte[] exportQuestions(List<Question> questions) {
        String[] headers = {"ID", "题干", "题型", "选项A", "选项B", "选项C", "选项D", "正确答案", "分值", "分类"};
        String[] fields = {"questionId", "questionContent", "questionType", "optionA", "optionB", "optionC", "optionD", "correctAnswer", "score", "category"};
        return buildWorkbook("题库导出", headers, questions, fields);
    }

    public byte[] exportResults(List<ExamResult> results) {
        Map<Integer, String> studentNames = results.stream()
                .map(ExamResult::getStudentId)
                .distinct()
                .map(userMapper::selectById)
                .filter(u -> u != null)
                .collect(Collectors.toMap(SysUser::getUserId, SysUser::getRealName));

        String[] headers = {"学生ID", "学生姓名", "得分", "正确题数", "错误题数", "提交时间"};
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("成绩导出");
        sheet.setColumnWidth(0, 12 * 256);
        sheet.setColumnWidth(1, 16 * 256);
        sheet.setColumnWidth(2, 10 * 256);
        sheet.setColumnWidth(3, 12 * 256);
        sheet.setColumnWidth(4, 12 * 256);
        sheet.setColumnWidth(5, 20 * 256);

        Row header = sheet.createRow(0);
        CellStyle headerStyle = headerStyle(wb);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        for (int i = 0; i < results.size(); i++) {
            ExamResult r = results.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(r.getStudentId());
            row.createCell(1).setCellValue(studentNames.getOrDefault(r.getStudentId(), "未知"));
            row.createCell(2).setCellValue(r.getTotalScore().doubleValue());
            row.createCell(3).setCellValue(r.getCorrectCount());
            row.createCell(4).setCellValue(r.getWrongCount());
            row.createCell(5).setCellValue(r.getSubmitTime() != null ? r.getSubmitTime().toString() : "");
        }

        return toBytes(wb);
    }

    private byte[] buildWorkbook(String sheetName, String[] headers, List<Question> data, String[] fields) {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet(sheetName);

        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, Math.max(12, headers[i].length() + 4) * 256);
        }

        Row header = sheet.createRow(0);
        CellStyle headerStyle = headerStyle(wb);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        for (int i = 0; i < data.size(); i++) {
            Question q = data.get(i);
            Row row = sheet.createRow(i + 1);
            for (int j = 0; j < fields.length; j++) {
                Cell cell = row.createCell(j);
                setCellValue(cell, q, fields[j]);
            }
        }

        return toBytes(wb);
    }

    private void setCellValue(Cell cell, Question q, String field) {
        switch (field) {
            case "questionId" -> cell.setCellValue(q.getQuestionId());
            case "questionContent" -> cell.setCellValue(q.getQuestionContent());
            case "questionType" -> cell.setCellValue(typeLabel(q.getQuestionType()));
            case "optionA" -> cell.setCellValue(q.getOptionA());
            case "optionB" -> cell.setCellValue(q.getOptionB());
            case "optionC" -> cell.setCellValue(q.getOptionC());
            case "optionD" -> cell.setCellValue(q.getOptionD());
            case "correctAnswer" -> cell.setCellValue(q.getCorrectAnswer());
            case "score" -> cell.setCellValue(q.getScore().doubleValue());
            case "category" -> cell.setCellValue(q.getCategory());
        }
    }

    private String typeLabel(String type) {
        if (type == null) return "";
        return switch (type) {
            case "single" -> "单选";
            case "judge" -> "判断";
            case "short_answer" -> "简答";
            case "fill_blank" -> "填空";
            default -> type;
        };
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    public byte[] exportWrongQuestions(List<Map<String, Object>> groups) {
        String[] headers = {"考试", "题目", "题型", "选项A", "选项B", "选项C", "选项D", "你的答案", "正确答案", "分值", "提交时间"};
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("错题导出");
        int[] widths = {20, 40, 8, 16, 16, 16, 16, 16, 16, 8, 20};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }

        Row header = sheet.createRow(0);
        CellStyle headerStyle = headerStyle(wb);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (Map<String, Object> group : groups) {
            String examName = (String) group.get("examName");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> questions = (List<Map<String, Object>>) group.get("questions");
            for (Map<String, Object> q : questions) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(examName != null ? examName : "");
                row.createCell(1).setCellValue(String.valueOf(q.getOrDefault("questionContent", "")));
                row.createCell(2).setCellValue(typeLabel((String) q.get("questionType")));
                row.createCell(3).setCellValue(String.valueOf(q.getOrDefault("optionA", "")));
                row.createCell(4).setCellValue(String.valueOf(q.getOrDefault("optionB", "")));
                row.createCell(5).setCellValue(String.valueOf(q.getOrDefault("optionC", "")));
                row.createCell(6).setCellValue(String.valueOf(q.getOrDefault("optionD", "")));
                row.createCell(7).setCellValue(String.valueOf(q.getOrDefault("studentAnswer", "")));
                row.createCell(8).setCellValue(String.valueOf(q.getOrDefault("correctAnswer", "")));
                Object score = q.get("score");
                row.createCell(9).setCellValue(score instanceof Number ? ((Number) score).doubleValue() : 0);
                Object st = q.get("submitTime");
                row.createCell(10).setCellValue(st != null ? st.toString() : "");
            }
        }

        return toBytes(wb);
    }

    public byte[] exportResultDetail(Map<String, Object> detail) {
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) detail.get("result");
        @SuppressWarnings("unchecked")
        Map<String, Object> examMap = (Map<String, Object>) detail.get("exam");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) detail.get("answers");

        String examName = examMap != null ? String.valueOf(examMap.getOrDefault("examName", "")) : "";
        Object totalScore = resultMap != null ? resultMap.get("totalScore") : 0;
        Object correctCount = resultMap != null ? resultMap.get("correctCount") : 0;
        Object wrongCount = resultMap != null ? resultMap.get("wrongCount") : 0;

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("成绩详情");

        sheet.setColumnWidth(0, 40 * 256);
        sheet.setColumnWidth(1, 8 * 256);
        sheet.setColumnWidth(2, 16 * 256);
        sheet.setColumnWidth(3, 16 * 256);
        sheet.setColumnWidth(4, 16 * 256);
        sheet.setColumnWidth(5, 16 * 256);
        sheet.setColumnWidth(6, 16 * 256);
        sheet.setColumnWidth(7, 16 * 256);
        sheet.setColumnWidth(8, 16 * 256);
        sheet.setColumnWidth(9, 8 * 256);

        // Exam info header
        Row infoRow = sheet.createRow(0);
        CellStyle infoStyle = wb.createCellStyle();
        Font infoFont = wb.createFont();
        infoFont.setBold(true);
        infoFont.setFontHeightInPoints((short) 12);
        infoStyle.setFont(infoFont);
        Cell infoCell = infoRow.createCell(0);
        infoCell.setCellValue(String.format("考试：%s    总分：%s    正确：%s    错误：%s",
                examName, totalScore, correctCount, wrongCount));
        infoCell.setCellStyle(infoStyle);

        // Header row
        String[] headers = {"题目", "题型", "选项A", "选项B", "选项C", "选项D", "你的答案", "正确答案", "是否正确", "分值"};
        Row header = sheet.createRow(1);
        CellStyle headerStyle = headerStyle(wb);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        for (int i = 0; i < answers.size(); i++) {
            Map<String, Object> a = answers.get(i);
            Row row = sheet.createRow(i + 2);
            row.createCell(0).setCellValue(String.valueOf(a.getOrDefault("questionContent", "")));
            row.createCell(1).setCellValue(typeLabel((String) a.get("questionType")));
            row.createCell(2).setCellValue(String.valueOf(a.getOrDefault("optionA", "")));
            row.createCell(3).setCellValue(String.valueOf(a.getOrDefault("optionB", "")));
            row.createCell(4).setCellValue(String.valueOf(a.getOrDefault("optionC", "")));
            row.createCell(5).setCellValue(String.valueOf(a.getOrDefault("optionD", "")));
            row.createCell(6).setCellValue(String.valueOf(a.getOrDefault("studentAnswer", "")));
            row.createCell(7).setCellValue(String.valueOf(a.getOrDefault("correctAnswer", "")));
            Object isCorrect = a.get("isCorrect");
            row.createCell(8).setCellValue(isCorrect instanceof Boolean && (Boolean) isCorrect ? "正确" : "错误");
            Object score = a.get("score");
            row.createCell(9).setCellValue(score instanceof Number ? ((Number) score).doubleValue() : 0);
        }

        return toBytes(wb);
    }

    private byte[] toBytes(Workbook wb) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            wb.write(os);
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }
}
