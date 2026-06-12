package com.example.answer.service;

import com.example.answer.common.BusinessException;
import com.example.answer.entity.Paper;
import com.example.answer.entity.StudentProfile;
import com.example.answer.repository.PaperRepository;
import com.example.answer.repository.StudentProfileRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class StudentExportService {

    private final StudentProfileRepository studentProfileRepository;
    private final PaperRepository paperRepository;
    private final PaperAccessService paperAccessService;

    public StudentExportService(
            StudentProfileRepository studentProfileRepository,
            PaperRepository paperRepository,
            PaperAccessService paperAccessService
    ) {
        this.studentProfileRepository = studentProfileRepository;
        this.paperRepository = paperRepository;
        this.paperAccessService = paperAccessService;
    }

    @Transactional(readOnly = true)
    public byte[] export(String keyword, String className, Long paperId) {
        Paper paper = paperId == null ? null : paperRepository.findById(paperId)
                .orElseThrow(() -> new BusinessException("试卷不存在"));
        if (paper != null) {
            paperAccessService.requireCurrentUserAccess(paper);
        }
        List<StudentProfile> students = studentProfileRepository.searchForExport(keyword).stream()
                .filter(student -> matchesClass(student, className))
                .filter(student -> matchesPaper(student, paper))
                .toList();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("学生信息");
            String[] headers = {"序号", "试卷", "学号", "姓名", "用户名", "班级", "年级", "学院", "专业", "状态"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            for (int i = 0; i < students.size(); i++) {
                StudentProfile student = students.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(paper == null ? "全部试卷" : paper.getTitle());
                row.createCell(2).setCellValue(value(student.getStudentNo()));
                row.createCell(3).setCellValue(value(student.getUser().getRealName()));
                row.createCell(4).setCellValue(value(student.getUser().getUsername()));
                row.createCell(5).setCellValue(value(student.getClassName()));
                row.createCell(6).setCellValue(value(student.getGrade()));
                row.createCell(7).setCellValue(value(student.getCollege()));
                row.createCell(8).setCellValue(value(student.getMajor()));
                row.createCell(9).setCellValue(Boolean.FALSE.equals(student.getUser().getEnabled()) ? "禁用" : "启用");
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("学生信息导出失败", ex);
        }
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private boolean matchesClass(StudentProfile student, String className) {
        if (!StringUtils.hasText(className)) {
            return true;
        }
        return className.trim().equals(student.getClassName());
    }

    private boolean matchesPaper(StudentProfile student, Paper paper) {
        if (paper == null) {
            return true;
        }
        return paperAccessService.isAvailableToMajor(paper, student.getMajor());
    }
}
