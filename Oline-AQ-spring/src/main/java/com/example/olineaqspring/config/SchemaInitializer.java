package com.example.olineaqspring.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public SchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        // Fix exam table columns
        jdbcTemplate.execute("ALTER TABLE exam ADD COLUMN IF NOT EXISTS allow_retake BOOLEAN DEFAULT FALSE");
        jdbcTemplate.execute("ALTER TABLE exam ADD COLUMN IF NOT EXISTS start_time TIMESTAMP");
        jdbcTemplate.execute("ALTER TABLE exam ADD COLUMN IF NOT EXISTS end_time TIMESTAMP");

        // Fix question table columns
        jdbcTemplate.execute("ALTER TABLE question ADD COLUMN IF NOT EXISTS category VARCHAR(100)");

        // Ensure all required tables exist
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS exam_history (" +
            "  history_id SERIAL PRIMARY KEY," +
            "  exam_id INTEGER NOT NULL," +
            "  operator_id INTEGER," +
            "  action_type VARCHAR(100)," +
            "  action_detail TEXT," +
            "  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );

        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS exam_question (" +
            "  id SERIAL PRIMARY KEY," +
            "  exam_id INTEGER NOT NULL," +
            "  question_id INTEGER NOT NULL," +
            "  sort_order INTEGER," +
            "  score NUMERIC(5,2)" +
            ")"
        );

        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS student_answer (" +
            "  answer_id SERIAL PRIMARY KEY," +
            "  exam_id INTEGER NOT NULL," +
            "  student_id INTEGER NOT NULL," +
            "  question_id INTEGER NOT NULL," +
            "  student_answer TEXT," +
            "  correct_answer TEXT," +
            "  is_correct BOOLEAN," +
            "  score NUMERIC(5,2)," +
            "  submit_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );

        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS exam_result (" +
            "  result_id SERIAL PRIMARY KEY," +
            "  exam_id INTEGER NOT NULL," +
            "  student_id INTEGER NOT NULL," +
            "  total_score NUMERIC(6,2)," +
            "  correct_count INTEGER," +
            "  wrong_count INTEGER," +
            "  use_time INTEGER," +
            "  submit_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );

        // Add assign_all to exam table
        jdbcTemplate.execute("ALTER TABLE exam ADD COLUMN IF NOT EXISTS assign_all BOOLEAN DEFAULT TRUE");

        // Create exam_student assignment table
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS exam_student (" +
            "  id SERIAL PRIMARY KEY," +
            "  exam_id INTEGER NOT NULL," +
            "  student_id INTEGER NOT NULL" +
            ")"
        );

        // Create system config table
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS sys_config (" +
            "  config_key VARCHAR(100) PRIMARY KEY," +
            "  config_value TEXT" +
            ")"
        );

        // Insert default config values if not exist
        jdbcTemplate.execute(
            "INSERT INTO sys_config (config_key, config_value) " +
            "SELECT 'storage.type', 'local' " +
            "WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'storage.type')"
        );

        // Create question_feedback table
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS question_feedback (" +
            "  feedback_id SERIAL PRIMARY KEY," +
            "  question_id INTEGER NOT NULL," +
            "  student_id INTEGER NOT NULL," +
            "  exam_id INTEGER," +
            "  feedback_type VARCHAR(50) NOT NULL," +
            "  description TEXT NOT NULL," +
            "  status VARCHAR(50) DEFAULT 'pending'," +
            "  reject_reason TEXT," +
            "  resolve_type VARCHAR(50)," +
            "  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );

        // Create wrong_notebook table
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS wrong_notebook (" +
            "  notebook_id SERIAL PRIMARY KEY," +
            "  student_id INTEGER NOT NULL," +
            "  notebook_name VARCHAR(100) NOT NULL," +
            "  description TEXT," +
            "  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );

        // Create wrong_notebook_item table
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS wrong_notebook_item (" +
            "  id SERIAL PRIMARY KEY," +
            "  notebook_id INTEGER NOT NULL," +
            "  answer_id INTEGER NOT NULL," +
            "  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );

        // Performance indexes
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_exam_question_exam_id ON exam_question(exam_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_exam_question_question_id ON exam_question(question_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_student_answer_exam_id ON student_answer(exam_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_student_answer_student_id ON student_answer(student_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_student_answer_question_id ON student_answer(question_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_exam_result_exam_id ON exam_result(exam_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_exam_result_student_id ON exam_result(student_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_exam_history_exam_id ON exam_history(exam_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_question_source_file_id ON question(source_file_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_question_category ON question(category)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_upload_file_status ON upload_file(status)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_question_feedback_question_id ON question_feedback(question_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_question_feedback_status ON question_feedback(status)");
    }
}
