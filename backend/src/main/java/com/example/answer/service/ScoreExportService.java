package com.example.answer.service;

import com.example.answer.entity.StudentProfile;
import com.example.answer.entity.Submission;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ScoreExportService {

    private final SubmissionService submissionService;

    public ScoreExportService(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @Transactional(readOnly = true)
    public byte[] export(Long paperId, String className) {
        List<Submission> submissions = submissionService.exportRows(paperId, className);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("成绩表");
            String[] headers = {"序号", "试卷", "学号", "姓名", "班级", "年级", "学院", "得分", "总分", "提交时间"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            for (int i = 0; i < submissions.size(); i++) {
                Submission submission = submissions.get(i);
                StudentProfile student = submission.getStudent();
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(submission.getPaper().getTitle());
                row.createCell(2).setCellValue(student.getStudentNo());
                row.createCell(3).setCellValue(student.getUser().getRealName());
                row.createCell(4).setCellValue(student.getClassName());
                row.createCell(5).setCellValue(student.getGrade());
                row.createCell(6).setCellValue(student.getCollege());
                row.createCell(7).setCellValue(submission.getScore().doubleValue());
                row.createCell(8).setCellValue(submission.getTotalScore().doubleValue());
                row.createCell(9).setCellValue(submission.getSubmittedAt().toString());
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("成绩表导出失败", ex);
        }
    }
}
