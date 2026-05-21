-- =============================================
-- 智能在线答题系统 - 数据库初始化脚本
-- 数据库：PostgreSQL
-- =============================================

CREATE TABLE IF NOT EXISTS sys_user (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(100),
    role VARCHAR(20),
    email VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS question (
    question_id SERIAL PRIMARY KEY,
    question_content TEXT,
    question_type VARCHAR(20),
    option_a TEXT,
    option_b TEXT,
    option_c TEXT,
    option_d TEXT,
    correct_answer TEXT,
    score NUMERIC(5,2),
    category VARCHAR(100),
    source_file_id INTEGER,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS exam (
    exam_id SERIAL PRIMARY KEY,
    exam_name VARCHAR(200),
    description TEXT,
    duration INTEGER,
    total_score NUMERIC(6,2),
    status VARCHAR(20) DEFAULT 'draft',
    allow_retake BOOLEAN DEFAULT FALSE,
    assign_all BOOLEAN DEFAULT TRUE,
    shuffle_questions BOOLEAN DEFAULT FALSE,
    shuffle_answers BOOLEAN DEFAULT FALSE,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS exam_question (
    id SERIAL PRIMARY KEY,
    exam_id INTEGER NOT NULL,
    question_id INTEGER NOT NULL,
    sort_order INTEGER,
    score NUMERIC(5,2)
);

CREATE TABLE IF NOT EXISTS exam_student (
    id SERIAL PRIMARY KEY,
    exam_id INTEGER NOT NULL,
    student_id INTEGER NOT NULL
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

CREATE TABLE IF NOT EXISTS exam_history (
    history_id SERIAL PRIMARY KEY,
    exam_id INTEGER NOT NULL,
    operator_id INTEGER,
    action_type VARCHAR(100),
    action_detail TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS upload_file (
    file_id SERIAL PRIMARY KEY,
    file_name VARCHAR(255),
    file_type VARCHAR(50),
    file_path TEXT,
    raw_text TEXT,
    upload_user_id INTEGER,
    status VARCHAR(20),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_config (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value TEXT
);

CREATE TABLE IF NOT EXISTS question_feedback (
    feedback_id SERIAL PRIMARY KEY,
    question_id INTEGER NOT NULL,
    student_id INTEGER NOT NULL,
    exam_id INTEGER,
    feedback_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'pending',
    reject_reason TEXT,
    resolve_type VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wrong_notebook (
    notebook_id SERIAL PRIMARY KEY,
    student_id INTEGER NOT NULL,
    notebook_name VARCHAR(100) NOT NULL,
    description TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wrong_notebook_item (
    id SERIAL PRIMARY KEY,
    notebook_id INTEGER NOT NULL,
    answer_id INTEGER NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS email_verification (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(6) NOT NULL,
    expire_time TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS smtp_account (
    id SERIAL PRIMARY KEY,
    host VARCHAR(255) NOT NULL,
    port INTEGER DEFAULT 587,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    from_address VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT FALSE,
    enabled BOOLEAN DEFAULT TRUE,
    hourly_limit INTEGER DEFAULT 100,
    daily_limit INTEGER DEFAULT 1000,
    hourly_sent INTEGER DEFAULT 0,
    daily_sent INTEGER DEFAULT 0,
    last_hourly_reset TIMESTAMP,
    last_daily_reset TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS email_send_log (
    id SERIAL PRIMARY KEY,
    account_id INTEGER,
    send_to VARCHAR(255) NOT NULL,
    send_type VARCHAR(50) NOT NULL,
    success BOOLEAN DEFAULT TRUE,
    send_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS announcement (
    announcement_id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS announcement_read (
    id SERIAL PRIMARY KEY,
    announcement_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    read_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 默认配置
INSERT INTO sys_config (config_key, config_value) SELECT 'storage.type', 'local' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'storage.type');
INSERT INTO sys_config (config_key, config_value) SELECT 'register.email.enabled', 'false' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'register.email.enabled');
INSERT INTO sys_config (config_key, config_value) SELECT 'smtp.host', '' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'smtp.host');
INSERT INTO sys_config (config_key, config_value) SELECT 'smtp.port', '587' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'smtp.port');
INSERT INTO sys_config (config_key, config_value) SELECT 'smtp.username', '' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'smtp.username');
INSERT INTO sys_config (config_key, config_value) SELECT 'smtp.password', '' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'smtp.password');
INSERT INTO sys_config (config_key, config_value) SELECT 'smtp.from', '' WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'smtp.from');

-- 索引
CREATE INDEX IF NOT EXISTS idx_exam_question_exam_id ON exam_question(exam_id);
CREATE INDEX IF NOT EXISTS idx_exam_question_question_id ON exam_question(question_id);
CREATE INDEX IF NOT EXISTS idx_student_answer_exam_id ON student_answer(exam_id);
CREATE INDEX IF NOT EXISTS idx_student_answer_student_id ON student_answer(student_id);
CREATE INDEX IF NOT EXISTS idx_student_answer_question_id ON student_answer(question_id);
CREATE INDEX IF NOT EXISTS idx_exam_result_exam_id ON exam_result(exam_id);
CREATE INDEX IF NOT EXISTS idx_exam_result_student_id ON exam_result(student_id);
CREATE INDEX IF NOT EXISTS idx_exam_history_exam_id ON exam_history(exam_id);
CREATE INDEX IF NOT EXISTS idx_question_source_file_id ON question(source_file_id);
CREATE INDEX IF NOT EXISTS idx_question_category ON question(category);
CREATE INDEX IF NOT EXISTS idx_upload_file_status ON upload_file(status);
CREATE INDEX IF NOT EXISTS idx_question_feedback_question_id ON question_feedback(question_id);
CREATE INDEX IF NOT EXISTS idx_question_feedback_status ON question_feedback(status);
CREATE INDEX IF NOT EXISTS idx_email_send_log_time ON email_send_log(send_time);
CREATE INDEX IF NOT EXISTS idx_email_send_log_account ON email_send_log(account_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_email ON sys_user(email) WHERE email IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_announcement_read_user ON announcement_read(user_id);
CREATE INDEX IF NOT EXISTS idx_announcement_read_announcement ON announcement_read(announcement_id);
