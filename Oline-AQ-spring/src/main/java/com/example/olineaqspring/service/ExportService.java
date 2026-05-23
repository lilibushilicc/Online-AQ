package com.example.olineaqspring.service;

import com.example.olineaqspring.entity.Exam;
import com.example.olineaqspring.entity.ExamResult;
import com.example.olineaqspring.entity.Question;
import com.example.olineaqspring.entity.SysUser;
import com.example.olineaqspring.mapper.ExamMapper;
import com.example.olineaqspring.mapper.UserMapper;
import com.example.olineaqspring.vo.ResultAnswerVO;
import com.example.olineaqspring.vo.ResultDetailVO;
import com.example.olineaqspring.vo.WrongQuestionGroupVO;
import com.example.olineaqspring.vo.WrongQuestionItemVO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
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
            if (q == null) continue;
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
            case "questionContent" -> cell.setCellValue(nullToEmpty(q.getQuestionContent()));
            case "questionType" -> cell.setCellValue(typeLabel(q.getQuestionType()));
            case "optionA" -> cell.setCellValue(nullToEmpty(q.getOptionA()));
            case "optionB" -> cell.setCellValue(nullToEmpty(q.getOptionB()));
            case "optionC" -> cell.setCellValue(nullToEmpty(q.getOptionC()));
            case "optionD" -> cell.setCellValue(nullToEmpty(q.getOptionD()));
            case "correctAnswer" -> cell.setCellValue(nullToEmpty(q.getCorrectAnswer()));
            case "score" -> cell.setCellValue(q.getScore() != null ? q.getScore().doubleValue() : 0);
            case "category" -> cell.setCellValue(nullToEmpty(q.getCategory()));
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

    public byte[] exportWrongQuestions(List<WrongQuestionGroupVO> groups) {
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
        for (WrongQuestionGroupVO group : groups) {
            String examName = group.getExamName();
            List<WrongQuestionItemVO> questions = group.getQuestions();
            for (WrongQuestionItemVO q : questions) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(examName != null ? examName : "");
                row.createCell(1).setCellValue(nullToEmpty(q.getQuestionContent()));
                row.createCell(2).setCellValue(typeLabel(q.getQuestionType()));
                row.createCell(3).setCellValue(nullToEmpty(q.getOptionA()));
                row.createCell(4).setCellValue(nullToEmpty(q.getOptionB()));
                row.createCell(5).setCellValue(nullToEmpty(q.getOptionC()));
                row.createCell(6).setCellValue(nullToEmpty(q.getOptionD()));
                row.createCell(7).setCellValue(nullToEmpty(q.getStudentAnswer()));
                row.createCell(8).setCellValue(nullToEmpty(q.getCorrectAnswer()));
                row.createCell(9).setCellValue(q.getScore() != null ? q.getScore().doubleValue() : 0);
                row.createCell(10).setCellValue(q.getSubmitTime() != null ? q.getSubmitTime().toString() : "");
            }
        }

        return toBytes(wb);
    }

    public byte[] exportResultDetail(ResultDetailVO detail) {
        ExamResult examResult = detail.getResult();
        Exam exam = detail.getExam();
        List<ResultAnswerVO> answers = detail.getAnswers();
        if (answers == null) answers = List.of();

        String examName = exam != null ? exam.getExamName() : "";
        Object totalScore = examResult != null ? examResult.getTotalScore() : 0;
        Object correctCount = examResult != null ? examResult.getCorrectCount() : 0;
        Object wrongCount = examResult != null ? examResult.getWrongCount() : 0;

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
            ResultAnswerVO a = answers.get(i);
            if (a == null) continue;
            Row row = sheet.createRow(i + 2);
            row.createCell(0).setCellValue(nullToEmpty(a.getQuestionContent()));
            row.createCell(1).setCellValue(typeLabel(a.getQuestionType()));
            row.createCell(2).setCellValue(nullToEmpty(a.getOptionA()));
            row.createCell(3).setCellValue(nullToEmpty(a.getOptionB()));
            row.createCell(4).setCellValue(nullToEmpty(a.getOptionC()));
            row.createCell(5).setCellValue(nullToEmpty(a.getOptionD()));
            row.createCell(6).setCellValue(nullToEmpty(a.getStudentAnswer()));
            row.createCell(7).setCellValue(nullToEmpty(a.getCorrectAnswer()));
            row.createCell(8).setCellValue(Boolean.TRUE.equals(a.getIsCorrect()) ? "正确" : "错误");
            row.createCell(9).setCellValue(a.getScore() != null ? a.getScore().doubleValue() : 0);
        }

        return toBytes(wb);
    }

    public byte[] exportResultDetailWord(ResultDetailVO detail) {
        ExamResult examResult = detail.getResult();
        Exam exam = detail.getExam();
        List<ResultAnswerVO> answers = detail.getAnswers();
        if (answers == null) answers = List.of();

        String examName = exam != null ? exam.getExamName() : "";
        Object totalScore = examResult != null ? examResult.getTotalScore() : 0;
        Object correctCount = examResult != null ? examResult.getCorrectCount() : 0;
        Object wrongCount = examResult != null ? examResult.getWrongCount() : 0;

        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            // Title
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("考试成绩详情");
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setFontFamily("微软雅黑");

            // Stats
            XWPFParagraph statsPara = doc.createParagraph();
            statsPara.setSpacingAfter(200);
            XWPFRun statsRun = statsPara.createRun();
            statsRun.setText(String.format("考试：%s    总分：%s    正确：%s    错误：%s",
                    examName, totalScore, correctCount, wrongCount));
            statsRun.setFontSize(11);
            statsRun.setFontFamily("微软雅黑");

            // Table
            String[] headers = {"序号", "题目", "题型", "选项A", "选项B", "选项C", "选项D", "你的答案", "正确答案", "是否正确", "分值"};
            int cols = headers.length;
            XWPFTable table = doc.createTable(answers.size() + 1, cols);
            table.setWidth("100%");

            // Column widths (approximate proportions)
            int[] widths = {8, 35, 8, 12, 12, 12, 12, 12, 12, 10, 8};
            setTableGridCols(table, cols, widths);

            // Header row
            XWPFTableRow headerRow = table.getRow(0);
            for (int i = 0; i < cols; i++) {
                XWPFParagraph p = headerRow.getCell(i).getParagraphs().get(0);
                p.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun r = p.createRun();
                r.setText(headers[i]);
                r.setBold(true);
                r.setFontSize(9);
                r.setFontFamily("微软雅黑");
                headerRow.getCell(i).setColor("E8E8E8");
            }

            // Data rows
            for (int i = 0; i < answers.size(); i++) {
                ResultAnswerVO a = answers.get(i);
                if (a == null) continue;
                XWPFTableRow row = table.getRow(i + 1);
                String[] vals = {
                        String.valueOf(i + 1),
                        nullToEmpty(a.getQuestionContent()),
                        typeLabel(a.getQuestionType()),
                        nullToEmpty(a.getOptionA()),
                        nullToEmpty(a.getOptionB()),
                        nullToEmpty(a.getOptionC()),
                        nullToEmpty(a.getOptionD()),
                        nullToEmpty(a.getStudentAnswer()),
                        nullToEmpty(a.getCorrectAnswer()),
                        Boolean.TRUE.equals(a.getIsCorrect()) ? "正确" : "错误",
                        String.valueOf(a.getScore() != null ? a.getScore() : "0"),
                };
                for (int j = 0; j < cols; j++) {
                    XWPFParagraph p = row.getCell(j).getParagraphs().get(0);
                    XWPFRun r = p.createRun();
                    r.setText(vals[j]);
                    r.setFontSize(9);
                    r.setFontFamily("微软雅黑");
                }
            }

            doc.write(os);
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("导出 Word 失败：" + e.getMessage());
        }
    }

    public byte[] exportExamPaperExcel(Map<String, Object> detail) {
        Exam exam = (Exam) detail.get("exam");
        @SuppressWarnings("unchecked")
        List<Question> questions = (List<Question>) detail.get("questions");
        if (questions == null) questions = List.of();

        String examName = exam != null ? exam.getExamName() : "";

        String[] headers = {"序号", "题型", "题目", "选项A", "选项B", "选项C", "选项D", "分值"};
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet(examName.length() > 31 ? examName.substring(0, 31) : examName);

        int[] widths = {8, 8, 40, 16, 16, 16, 16, 8};
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

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            if (q == null) continue;
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(typeLabel(q.getQuestionType()));
            row.createCell(2).setCellValue(nullToEmpty(q.getQuestionContent()));
            row.createCell(3).setCellValue(nullToEmpty(q.getOptionA()));
            row.createCell(4).setCellValue(nullToEmpty(q.getOptionB()));
            row.createCell(5).setCellValue(nullToEmpty(q.getOptionC()));
            row.createCell(6).setCellValue(nullToEmpty(q.getOptionD()));
            row.createCell(7).setCellValue(q.getScore() != null ? q.getScore().doubleValue() : 0);
        }

        return toBytes(wb);
    }

    public byte[] exportExamPaperWord(Map<String, Object> detail) {
        Exam exam = (Exam) detail.get("exam");
        @SuppressWarnings("unchecked")
        List<Question> questions = (List<Question>) detail.get("questions");
        if (questions == null) questions = List.of();

        String examName = exam != null ? exam.getExamName() : "";
        String description = exam != null ? exam.getDescription() : "";

        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(examName);
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setFontFamily("微软雅黑");

            if (description != null && !description.isEmpty() && !"null".equals(description)) {
                XWPFParagraph descPara = doc.createParagraph();
                descPara.setSpacingAfter(200);
                XWPFRun descRun = descPara.createRun();
                descRun.setText(description);
                descRun.setFontSize(11);
                descRun.setFontFamily("微软雅黑");
                descRun.setItalic(true);
            }

            String[] headers = {"序号", "题型", "题目", "选项A", "选项B", "选项C", "选项D", "分值"};
            int cols = headers.length;
            XWPFTable table = doc.createTable(questions.size() + 1, cols);
            table.setWidth("100%");

            int[] widths = {8, 8, 35, 12, 12, 12, 12, 8};
            setTableGridCols(table, cols, widths);

            XWPFTableRow headerRow = table.getRow(0);
            for (int i = 0; i < cols; i++) {
                XWPFParagraph p = headerRow.getCell(i).getParagraphs().get(0);
                p.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun r = p.createRun();
                r.setText(headers[i]);
                r.setBold(true);
                r.setFontSize(9);
                r.setFontFamily("微软雅黑");
                headerRow.getCell(i).setColor("E8E8E8");
            }

            for (int i = 0; i < questions.size(); i++) {
                Question q = questions.get(i);
                if (q == null) continue;
                XWPFTableRow row = table.getRow(i + 1);
                String[] vals = {
                        String.valueOf(i + 1),
                        typeLabel(q.getQuestionType()),
                        nullToEmpty(q.getQuestionContent()),
                        nullToEmpty(q.getOptionA()),
                        nullToEmpty(q.getOptionB()),
                        nullToEmpty(q.getOptionC()),
                        nullToEmpty(q.getOptionD()),
                        q.getScore() != null ? String.valueOf(q.getScore()) : "0",
                };
                for (int j = 0; j < cols; j++) {
                    XWPFParagraph p = row.getCell(j).getParagraphs().get(0);
                    XWPFRun r = p.createRun();
                    r.setText(vals[j]);
                    r.setFontSize(9);
                    r.setFontFamily("微软雅黑");
                }
            }

            doc.write(os);
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("导出试卷 Word 失败：" + e.getMessage());
        }
    }

    private static void setTableGridCols(XWPFTable table, int cols, int[] widths) {
        CTTblGrid grid = table.getCTTbl().getTblGrid();
        if (grid == null) {
            grid = table.getCTTbl().addNewTblGrid();
        }
        for (int i = 0; i < cols; i++) {
            grid.addNewGridCol();
            grid.getGridColArray()[i].setW(BigInteger.valueOf(widths[i] * 200L));
        }
    }

    private byte[] toBytes(Workbook wb) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            wb.write(os);
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
