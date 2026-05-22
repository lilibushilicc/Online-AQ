CREATE TABLE IF NOT EXISTS sys_user (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(100),
    role VARCHAR(50) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS upload_file (
    file_id SERIAL PRIMARY KEY,
    file_name VARCHAR(255),
    file_type VARCHAR(50),
    file_path VARCHAR(500),
    raw_text TEXT,
    upload_user_id INTEGER,
    status VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS question (
    question_id SERIAL PRIMARY KEY,
    question_content TEXT NOT NULL,
    question_type VARCHAR(50),
    option_a TEXT,
    option_b TEXT,
    option_c TEXT,
    option_d TEXT,
    correct_answer VARCHAR(255),
    score NUMERIC(5,2) DEFAULT 5,
    source_file_id INTEGER,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS exam (
    exam_id SERIAL PRIMARY KEY,
    exam_name VARCHAR(255) NOT NULL,
    description TEXT,
    duration INTEGER,
    total_score NUMERIC(6,2),
    status VARCHAR(50),
    allow_retake BOOLEAN DEFAULT FALSE,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS exam_history (
    history_id SERIAL PRIMARY KEY,
    exam_id INTEGER NOT NULL,
    operator_id INTEGER,
    action_type VARCHAR(100),
    action_detail TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE exam ADD COLUMN IF NOT EXISTS allow_retake BOOLEAN DEFAULT FALSE;
ALTER TABLE exam ADD COLUMN IF NOT EXISTS start_time TIMESTAMP;
ALTER TABLE exam ADD COLUMN IF NOT EXISTS end_time TIMESTAMP;
ALTER TABLE exam ADD COLUMN IF NOT EXISTS shuffle_questions BOOLEAN DEFAULT FALSE;
ALTER TABLE exam ADD COLUMN IF NOT EXISTS shuffle_answers BOOLEAN DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS exam_question (
    id SERIAL PRIMARY KEY,
    exam_id INTEGER NOT NULL,
    question_id INTEGER NOT NULL,
    sort_order INTEGER,
    score NUMERIC(5,2)
);

CREATE TABLE IF NOT EXISTS student_answer (
    answer_id SERIAL PRIMARY KEY,
    exam_id INTEGER NOT NULL,
    student_id INTEGER NOT NULL,
    question_id INTEGER NOT NULL,
    student_answer TEXT,
    correct_answer TEXT,
    is_correct BOOLEAN,
    score NUMERIC(5,2),
    submit_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS exam_result (
    result_id SERIAL PRIMARY KEY,
    exam_id INTEGER NOT NULL,
    student_id INTEGER NOT NULL,
    total_score NUMERIC(6,2),
    correct_count INTEGER,
    wrong_count INTEGER,
    use_time INTEGER,
    submit_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 防重复提交：同一学生同一考试只能有一条结果记录
ALTER TABLE exam_result ADD CONSTRAINT IF NOT EXISTS uk_exam_result_exam_student UNIQUE (exam_id, student_id);

-- 幂等记录表：防重复提交的通用兜底
CREATE TABLE IF NOT EXISTS idempotent_record (
    idempotent_key VARCHAR(128) PRIMARY KEY,
    result_type VARCHAR(64) NOT NULL,
    result_json TEXT,
    expire_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_idempotent_expire ON idempotent_record (expire_time);

-- 密码使用 BCrypt 加密（明文 123456 的加密结果）
-- 应用启动时会通过 DemoUserInitializer 自动创建用户（使用 BCrypt 加密）
-- 如需手动插入，请使用编程方式生成加密密码

INSERT INTO question (question_content, question_type, option_a, option_b, option_c, option_d, correct_answer, score)
SELECT 'Java属于哪种语言？', 'single', '前端语言', '后端语言', '数据库语言', '标记语言', 'B', 5
WHERE NOT EXISTS (SELECT 1 FROM question WHERE question_content = 'Java属于哪种语言？');

INSERT INTO question (question_content, question_type, option_a, option_b, option_c, option_d, correct_answer, score)
SELECT 'Vue主要用于什么？', 'single', '前端页面开发', '数据库管理', '服务器运维', '图片处理', 'A', 5
WHERE NOT EXISTS (SELECT 1 FROM question WHERE question_content = 'Vue主要用于什么？');

INSERT INTO question (question_content, question_type, option_a, option_b, option_c, option_d, correct_answer, score)
SELECT 'Spring Boot主要用于什么？', 'single', '后端开发', '图片编辑', '文档排版', '视频剪辑', 'A', 5
WHERE NOT EXISTS (SELECT 1 FROM question WHERE question_content = 'Spring Boot主要用于什么？');
