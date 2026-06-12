package com.example.answer.service;

import com.example.answer.common.BusinessException;
import com.example.answer.dto.StudentClassOptionDto;
import com.example.answer.dto.StudentClassOptionRequest;
import com.example.answer.entity.StudentClassOption;
import com.example.answer.repository.StudentClassOptionRepository;
import com.example.answer.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class StudentClassOptionService {

    private final StudentClassOptionRepository studentClassOptionRepository;
    private final StudentProfileRepository studentProfileRepository;

    public StudentClassOptionService(
            StudentClassOptionRepository studentClassOptionRepository,
            StudentProfileRepository studentProfileRepository
    ) {
        this.studentClassOptionRepository = studentClassOptionRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentClassOptionDto> list() {
        Map<String, StudentClassOptionDto> options = new TreeMap<>();
        for (StudentClassOption option : studentClassOptionRepository.findAllByOrderByCollegeAscMajorAscClassNameAsc()) {
            putOption(options, new StudentClassOptionDto(option.getId(), option.getCollege(), option.getMajor(), option.getClassName()));
        }
        for (Object[] triple : studentProfileRepository.findDistinctCollegeMajorClassTriples()) {
            String college = (String) triple[0];
            String major = (String) triple[1];
            String className = (String) triple[2];
            putOption(options, new StudentClassOptionDto(null, college, major, className));
        }
        return new ArrayList<>(options.values());
    }

    @Transactional
    public StudentClassOptionDto create(StudentClassOptionRequest request) {
        String college = normalize(request.college(), "学院不能为空");
        String major = normalize(request.major(), "专业不能为空");
        String className = normalize(request.className(), "班级不能为空");
        if (studentClassOptionRepository.existsByCollegeAndMajorAndClassName(college, major, className)) {
            throw new BusinessException("该学院、专业和班级已存在");
        }
        StudentClassOption option = new StudentClassOption();
        option.setCollege(college);
        option.setMajor(major);
        option.setClassName(className);
        return toDto(studentClassOptionRepository.save(option));
    }

    @Transactional
    public void delete(Long id) {
        if (!studentClassOptionRepository.existsById(id)) {
            throw new BusinessException("学院专业班级不存在");
        }
        studentClassOptionRepository.deleteById(id);
    }

    private void putOption(Map<String, StudentClassOptionDto> options, StudentClassOptionDto option) {
        if (!StringUtils.hasText(option.college()) || !StringUtils.hasText(option.major()) || !StringUtils.hasText(option.className())) {
            return;
        }
        String key = option.college() + "\n" + option.major() + "\n" + option.className();
        options.putIfAbsent(key, option);
    }

    private StudentClassOptionDto toDto(StudentClassOption option) {
        return new StudentClassOptionDto(option.getId(), option.getCollege(), option.getMajor(), option.getClassName());
    }

    private String normalize(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
        return value.trim();
    }
}
