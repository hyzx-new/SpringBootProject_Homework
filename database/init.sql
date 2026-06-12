CREATE DATABASE IF NOT EXISTS student_answer_system
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE student_answer_system;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  `role` VARCHAR(20) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  avatar_url VARCHAR(500) NULL,
  major VARCHAR(80) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500) NULL AFTER enabled',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND COLUMN_NAME = 'avatar_url'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE users ADD COLUMN major VARCHAR(80) NULL AFTER avatar_url',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND COLUMN_NAME = 'major'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS student_profiles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  student_no VARCHAR(30) NOT NULL UNIQUE,
  class_name VARCHAR(50) NOT NULL,
  grade VARCHAR(30) NOT NULL,
  college VARCHAR(80) NOT NULL,
  major VARCHAR(80) NOT NULL DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_student_profiles_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE student_profiles ADD COLUMN major VARCHAR(80) NOT NULL DEFAULT '''' AFTER college',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'student_profiles'
    AND COLUMN_NAME = 'major'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS student_class_options (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  college VARCHAR(80) NOT NULL,
  major VARCHAR(80) NOT NULL DEFAULT '',
  class_name VARCHAR(50) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_student_class_options_college_major_class (college, major, class_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE student_class_options ADD COLUMN major VARCHAR(80) NOT NULL DEFAULT '''' AFTER college',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'student_class_options'
    AND COLUMN_NAME = 'major'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE student_class_options sco
JOIN (
  SELECT college, class_name, MIN(major) AS major
  FROM student_profiles
  WHERE college IS NOT NULL
    AND college <> ''
    AND major IS NOT NULL
    AND major <> ''
    AND class_name IS NOT NULL
    AND class_name <> ''
  GROUP BY college, class_name
) profile_options
  ON profile_options.college = sco.college
 AND profile_options.class_name = sco.class_name
SET sco.major = profile_options.major
WHERE sco.major = '';

UPDATE student_class_options
SET major = TRIM(REGEXP_REPLACE(class_name, '[[:space:]]*[0-9０-９一二三四五六七八九十]+[[:space:]]*班$', ''))
WHERE major = '';

UPDATE student_class_options
SET major = class_name
WHERE major = '';

SET @sql = (
  SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE student_class_options DROP INDEX uk_student_class_options_college_class',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'student_class_options'
    AND INDEX_NAME = 'uk_student_class_options_college_class'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE student_class_options ADD UNIQUE KEY uk_student_class_options_college_major_class (college, major, class_name)',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'student_class_options'
    AND INDEX_NAME = 'uk_student_class_options_college_major_class'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO student_class_options (college, major, class_name)
SELECT DISTINCT college, major, class_name
FROM student_profiles
WHERE college IS NOT NULL
  AND college <> ''
  AND major IS NOT NULL
  AND major <> ''
  AND class_name IS NOT NULL
  AND class_name <> '';

CREATE TABLE IF NOT EXISTS papers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(120) NOT NULL,
  description TEXT,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  total_score DECIMAL(8, 2) NOT NULL DEFAULT 0,
  published TINYINT(1) NOT NULL DEFAULT 1,
  major VARCHAR(80) NULL,
  common_course TINYINT(1) NOT NULL DEFAULT 0,
  created_by_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_papers_created_by
    FOREIGN KEY (created_by_id) REFERENCES users(id)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE papers ADD COLUMN major VARCHAR(80) NULL AFTER published',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'papers'
    AND COLUMN_NAME = 'major'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE papers ADD COLUMN common_course TINYINT(1) NOT NULL DEFAULT 0 AFTER major',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'papers'
    AND COLUMN_NAME = 'common_course'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE student_profiles
SET major = '软件工程'
WHERE id = 1
  AND major = '';

UPDATE users
SET major = '软件工程'
WHERE id = 2
  AND `role` = 'TEACHER'
  AND (major IS NULL OR major = '');

UPDATE papers
SET common_course = 1
WHERE common_course = 0
  AND (major IS NULL OR major = '');

CREATE TABLE IF NOT EXISTS questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  paper_id BIGINT NOT NULL,
  title VARCHAR(500) NOT NULL,
  type VARCHAR(30) NOT NULL,
  options_text TEXT,
  correct_answer TEXT NOT NULL,
  score DECIMAL(8, 2) NOT NULL,
  order_no INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_questions_paper (paper_id),
  CONSTRAINT fk_questions_paper
    FOREIGN KEY (paper_id) REFERENCES papers(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE questions MODIFY correct_answer TEXT NOT NULL;

CREATE TABLE IF NOT EXISTS submissions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  paper_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  score DECIMAL(8, 2) NOT NULL,
  total_score DECIMAL(8, 2) NOT NULL,
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_submissions_paper (paper_id),
  INDEX idx_submissions_student (student_id),
  CONSTRAINT fk_submissions_paper
    FOREIGN KEY (paper_id) REFERENCES papers(id),
  CONSTRAINT fk_submissions_student
    FOREIGN KEY (student_id) REFERENCES student_profiles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS student_answers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  submission_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  answer_text TEXT,
  correct_answer TEXT NOT NULL,
  correct_flag TINYINT(1) NOT NULL,
  score DECIMAL(8, 2) NOT NULL,
  INDEX idx_student_answers_submission (submission_id),
  CONSTRAINT fk_student_answers_submission
    FOREIGN KEY (submission_id) REFERENCES submissions(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_student_answers_question
    FOREIGN KEY (question_id) REFERENCES questions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE student_answers MODIFY answer_text TEXT NULL;
ALTER TABLE student_answers MODIFY correct_answer TEXT NOT NULL;

CREATE TABLE IF NOT EXISTS exam_reminders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  paper_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  message VARCHAR(500) NOT NULL,
  remind_at DATETIME NOT NULL,
  read_flag TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_exam_reminders_paper_student (paper_id, student_id),
  INDEX idx_exam_reminders_student (student_id),
  CONSTRAINT fk_exam_reminders_paper
    FOREIGN KEY (paper_id) REFERENCES papers(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_exam_reminders_student
    FOREIGN KEY (student_id) REFERENCES student_profiles(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS uploaded_files (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  original_name VARCHAR(255) NOT NULL,
  stored_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(120),
  file_size BIGINT NOT NULL,
  storage_path VARCHAR(500) NOT NULL,
  uploader_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_uploaded_files_uploader (uploader_id),
  CONSTRAINT fk_uploaded_files_uploader
    FOREIGN KEY (uploader_id) REFERENCES users(id)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS discussion_posts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(160) NOT NULL,
  content TEXT NOT NULL,
  paper_id BIGINT NULL,
  author_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_discussion_posts_paper (paper_id),
  INDEX idx_discussion_posts_author (author_id),
  CONSTRAINT fk_discussion_posts_paper
    FOREIGN KEY (paper_id) REFERENCES papers(id)
    ON DELETE SET NULL,
  CONSTRAINT fk_discussion_posts_author
    FOREIGN KEY (author_id) REFERENCES users(id)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS discussion_replies (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  author_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_discussion_replies_post (post_id),
  INDEX idx_discussion_replies_author (author_id),
  CONSTRAINT fk_discussion_replies_post
    FOREIGN KEY (post_id) REFERENCES discussion_posts(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_discussion_replies_author
    FOREIGN KEY (author_id) REFERENCES users(id)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO users (id, username, password_hash, real_name, `role`, enabled, avatar_url, major)
VALUES
  (1, 'admin', '$2a$10$KhMHXZ1dnGpbgNgmK7DBjOgGq.IF2NcDXoZiOVb1YeSDwBvUZsCPi', '系统管理员', 'ADMIN', 1, NULL, NULL),
  (2, 'teacher', '$2a$10$KhMHXZ1dnGpbgNgmK7DBjOgGq.IF2NcDXoZiOVb1YeSDwBvUZsCPi', '李老师', 'TEACHER', 1, NULL, '软件工程'),
  (3, 'student', '$2a$10$KhMHXZ1dnGpbgNgmK7DBjOgGq.IF2NcDXoZiOVb1YeSDwBvUZsCPi', '王同学', 'STUDENT', 1, NULL, NULL)
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  real_name = VALUES(real_name),
  `role` = VALUES(`role`),
  enabled = VALUES(enabled),
  avatar_url = VALUES(avatar_url),
  major = VALUES(major);

INSERT INTO student_profiles (id, user_id, student_no, class_name, grade, college, major)
VALUES
  (1, 3, '2052443662', '软件工程 1 班', '2025 级', '计算机学院', '软件工程')
ON DUPLICATE KEY UPDATE
  student_no = VALUES(student_no),
  class_name = VALUES(class_name),
  grade = VALUES(grade),
  college = VALUES(college),
  major = VALUES(major);

INSERT IGNORE INTO student_class_options (college, major, class_name)
SELECT DISTINCT college, major, class_name
FROM student_profiles
WHERE college IS NOT NULL
  AND college <> ''
  AND major IS NOT NULL
  AND major <> ''
  AND class_name IS NOT NULL
  AND class_name <> '';

INSERT INTO papers (id, title, description, start_time, end_time, total_score, published, major, common_course, created_by_id)
VALUES
  (1, 'Java 基础在线测试', '演示在线答题、自动判分、简答题人工批改和成绩导出。', '2026-06-01 09:00:00', '2026-12-31 23:59:59', 50.00, 1, '软件工程', 0, 2),
  (2, '数据结构预告测试', '开始前三天自动生成提醒的示例试卷。', '2026-06-10 09:00:00', '2026-06-10 11:00:00', 10.00, 1, NULL, 1, 2)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  description = VALUES(description),
  start_time = VALUES(start_time),
  end_time = VALUES(end_time),
  total_score = VALUES(total_score),
  published = VALUES(published),
  major = VALUES(major),
  common_course = VALUES(common_course),
  created_by_id = VALUES(created_by_id);

INSERT INTO questions (id, paper_id, title, type, options_text, correct_answer, score, order_no)
VALUES
  (1, 1, 'Spring Boot 3 默认使用哪一个 Jakarta 命名空间？', 'SINGLE_CHOICE',
    CONCAT('A. javax', CHAR(10), 'B. jakarta', CHAR(10), 'C. spring', CHAR(10), 'D. oracle'),
    'B', 10.00, 1),
  (2, 1, '下面哪些能力属于 Spring Boot 的常见优势？', 'MULTIPLE_CHOICE',
    CONCAT('A. 自动配置', CHAR(10), 'B. 必须手写所有 XML', CHAR(10), 'C. Starter 依赖', CHAR(10), 'D. 不支持 Web 应用'),
    'A,C', 10.00, 2),
  (3, 1, 'Redis 可以用于保存登录 token。', 'TRUE_FALSE',
    CONCAT('true. 正确', CHAR(10), 'false. 错误'),
    'true', 10.00, 3),
  (4, 1, 'MySQL 中主键自增常用关键字是什么？', 'FILL_BLANK',
    NULL,
    'AUTO_INCREMENT', 10.00, 4),
  (5, 1, '简述 Spring Boot Starter 的作用。', 'SHORT_ANSWER',
    NULL,
    'Starter 用于聚合常用依赖并配合自动配置减少样板配置。', 10.00, 5),
  (6, 2, '栈的典型特点是什么？', 'SINGLE_CHOICE',
    CONCAT('A. 先进先出', CHAR(10), 'B. 后进先出', CHAR(10), 'C. 随机访问', CHAR(10), 'D. 哈希映射'),
    'B', 10.00, 1)
ON DUPLICATE KEY UPDATE
  paper_id = VALUES(paper_id),
  title = VALUES(title),
  type = VALUES(type),
  options_text = VALUES(options_text),
  correct_answer = VALUES(correct_answer),
  score = VALUES(score),
  order_no = VALUES(order_no);

INSERT INTO exam_reminders (id, paper_id, student_id, message, remind_at, read_flag)
VALUES
  (1, 2, 1, '试卷《数据结构预告测试》将在 2026-06-10 09:00 开始，请提前准备。', '2026-06-07 08:00:00', 0)
ON DUPLICATE KEY UPDATE
  message = VALUES(message),
  remind_at = VALUES(remind_at),
  read_flag = VALUES(read_flag);

INSERT INTO discussion_posts (id, title, content, paper_id, author_id)
VALUES
  (1, 'Java 基础测试答题说明', '单选、判断和填空会自动判分；简答题提交后由教师在成绩详情中批改。', 1, 2)
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  content = VALUES(content),
  paper_id = VALUES(paper_id),
  author_id = VALUES(author_id);

INSERT INTO discussion_replies (id, post_id, content, author_id)
VALUES
  (1, 1, '收到，我会在答题后查看成绩详情。', 3)
ON DUPLICATE KEY UPDATE
  content = VALUES(content),
  author_id = VALUES(author_id);
