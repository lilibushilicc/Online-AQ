package com.example.olineaqspring.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class SchemaInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public SchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        addColumn("exam", "allow_retake BOOLEAN DEFAULT FALSE");
        addColumn("exam", "start_time TIMESTAMP");
        addColumn("exam", "end_time TIMESTAMP");
        addColumn("exam", "assign_all BOOLEAN DEFAULT TRUE");

        addColumn("question", "category VARCHAR(100)");
        addColumn("sys_user", "email VARCHAR(255)");

        createTable("exam_history",
            "history_id SERIAL PRIMARY KEY",
            "exam_id INTEGER NOT NULL",
            "operator_id INTEGER",
            "action_type VARCHAR(100)",
            "action_detail TEXT",
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        );
        createTable("exam_question",
            "id SERIAL PRIMARY KEY",
            "exam_id INTEGER NOT NULL",
            "question_id INTEGER NOT NULL",
            "sort_order INTEGER",
            "score NUMERIC(5,2)"
        );
        createTable("student_answer",
            "answer_id SERIAL PRIMARY KEY",
            "exam_id INTEGER NOT NULL",
            "student_id INTEGER NOT NULL",
            "question_id INTEGER NOT NULL",
            "student_answer TEXT",
            "correct_answer TEXT",
            "is_correct BOOLEAN",
            "score NUMERIC(5,2)",
            "submit_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        );
        createTable("exam_result",
            "result_id SERIAL PRIMARY KEY",
            "exam_id INTEGER NOT NULL",
            "student_id INTEGER NOT NULL",
            "total_score NUMERIC(6,2)",
            "correct_count INTEGER",
            "wrong_count INTEGER",
            "use_time INTEGER",
            "submit_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        );
        createTable("exam_student",
            "id SERIAL PRIMARY KEY",
            "exam_id INTEGER NOT NULL",
            "student_id INTEGER NOT NULL"
        );
        createTable("sys_config",
            "config_key VARCHAR(100) PRIMARY KEY",
            "config_value TEXT"
        );
        createTable("question_feedback",
            "feedback_id SERIAL PRIMARY KEY",
            "question_id INTEGER NOT NULL",
            "student_id INTEGER NOT NULL",
            "exam_id INTEGER",
            "feedback_type VARCHAR(50) NOT NULL",
            "description TEXT NOT NULL",
            "status VARCHAR(50) DEFAULT 'pending'",
            "reject_reason TEXT",
            "resolve_type VARCHAR(50)",
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP",
            "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        );
        createTable("wrong_notebook",
            "notebook_id SERIAL PRIMARY KEY",
            "student_id INTEGER NOT NULL",
            "notebook_name VARCHAR(100) NOT NULL",
            "description TEXT",
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        );
        createTable("wrong_notebook_item",
            "id SERIAL PRIMARY KEY",
            "notebook_id INTEGER NOT NULL",
            "answer_id INTEGER NOT NULL",
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        );
        createTable("email_verification",
            "id SERIAL PRIMARY KEY",
            "email VARCHAR(255) NOT NULL",
            "code VARCHAR(6) NOT NULL",
            "expire_time TIMESTAMP NOT NULL",
            "used BOOLEAN DEFAULT FALSE",
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        );
        createTable("smtp_account",
            "id SERIAL PRIMARY KEY",
            "host VARCHAR(255) NOT NULL",
            "port INTEGER DEFAULT 587",
            "username VARCHAR(255) NOT NULL",
            "password VARCHAR(255) NOT NULL",
            "from_address VARCHAR(255) NOT NULL",
            "active BOOLEAN DEFAULT FALSE",
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        );
        createTable("email_send_log",
            "id SERIAL PRIMARY KEY",
            "account_id INTEGER",
            "send_to VARCHAR(255) NOT NULL",
            "send_type VARCHAR(50) NOT NULL",
            "success BOOLEAN DEFAULT TRUE",
            "send_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        );

        insertConfig("storage.type", "local");
        insertConfig("register.email.enabled", "false");
        insertConfig("smtp.host", "");
        insertConfig("smtp.port", "587");
        insertConfig("smtp.username", "");
        insertConfig("smtp.password", "");
        insertConfig("smtp.from", "");
        insertConfig("smtp.subject", "${systemName} - 邮箱验证");
        insertConfig("smtp.system_name", "Online-AQ 智能在线答题系统");
        insertConfig("smtp.email_template", EmailConstants.DEFAULT_HTML_TEMPLATE);

        // Migrate old SMTP config to smtp_account if not yet migrated
        jdbcTemplate.execute(
            "INSERT INTO smtp_account (host, port, username, password, from_address, active) " +
            "SELECT COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'smtp.host'), ''), " +
            "  COALESCE((SELECT config_value::integer FROM sys_config WHERE config_key = 'smtp.port'), 587), " +
            "  COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'smtp.username'), ''), " +
            "  COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'smtp.password'), ''), " +
            "  COALESCE((SELECT config_value FROM sys_config WHERE config_key = 'smtp.from'), ''), " +
            "  TRUE " +
            "WHERE NOT EXISTS (SELECT 1 FROM smtp_account) " +
            "  AND EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'smtp.host' AND config_value != '')"
        );

        addColumn("smtp_account", "enabled BOOLEAN DEFAULT TRUE");
        addColumn("smtp_account", "hourly_limit INTEGER DEFAULT 100");
        addColumn("smtp_account", "daily_limit INTEGER DEFAULT 1000");
        addColumn("smtp_account", "hourly_sent INTEGER DEFAULT 0");
        addColumn("smtp_account", "daily_sent INTEGER DEFAULT 0");
        addColumn("smtp_account", "last_hourly_reset TIMESTAMP");
        addColumn("smtp_account", "last_daily_reset TIMESTAMP");
        addColumn("email_send_log", "account_id INTEGER");

        createIndex("idx_email_send_log_time", "email_send_log(send_time)");
        createIndex("idx_email_send_log_account", "email_send_log(account_id)");
        createIndex("idx_sys_user_email", "sys_user(email)", "WHERE email IS NOT NULL");
        createIndex("idx_exam_question_exam_id", "exam_question(exam_id)");
        createIndex("idx_exam_question_question_id", "exam_question(question_id)");
        createIndex("idx_student_answer_exam_id", "student_answer(exam_id)");
        createIndex("idx_student_answer_student_id", "student_answer(student_id)");
        createIndex("idx_student_answer_question_id", "student_answer(question_id)");
        createIndex("idx_exam_result_exam_id", "exam_result(exam_id)");
        createIndex("idx_exam_result_student_id", "exam_result(student_id)");
        createIndex("idx_exam_history_exam_id", "exam_history(exam_id)");
        createIndex("idx_question_source_file_id", "question(source_file_id)");
        createIndex("idx_question_category", "question(category)");
        createIndex("idx_upload_file_status", "upload_file(status)");
        createTable("announcement",
            "announcement_id SERIAL PRIMARY KEY",
            "title VARCHAR(200) NOT NULL",
            "content TEXT NOT NULL",
            "active BOOLEAN DEFAULT TRUE",
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP",
            "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        );
        createTable("announcement_read",
            "id SERIAL PRIMARY KEY",
            "announcement_id INTEGER NOT NULL",
            "user_id INTEGER NOT NULL",
            "read_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        );
        createIndex("idx_announcement_read_user", "announcement_read(user_id)");
        createIndex("idx_announcement_read_announcement", "announcement_read(announcement_id)");
        createIndex("idx_question_feedback_question_id", "question_feedback(question_id)");
        createIndex("idx_question_feedback_status", "question_feedback(status)");

        // 防重复提交：同一学生同一考试只能有一条结果记录
        addUniqueConstraintSafe("exam_result", "uk_exam_result_exam_student", "exam_id", "student_id");

        // 幂等记录表：通用幂等兜底
        createTable("idempotent_record",
            "idempotent_key VARCHAR(128) PRIMARY KEY",
            "result_type VARCHAR(64) NOT NULL",
            "result_json TEXT",
            "expire_time TIMESTAMP NOT NULL",
            "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        );
        createIndex("idx_idempotent_expire", "idempotent_record(expire_time)");
    }

    private void addUniqueConstraintSafe(String table, String constraintName, String... columns) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + constraintName +
                " UNIQUE (" + String.join(", ", columns) + ")");
        } catch (Exception e) {
            // 约束可能已存在，忽略错误
        }
    }

    private void createTable(String name, String... columns) {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + name + " (" +
            String.join(",\n  ", columns) + ")");
    }

    private void addColumn(String table, String definition) {
        jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS " + definition);
    }

    private void insertConfig(String key, String value) {
        jdbcTemplate.update(
            "INSERT INTO sys_config (config_key, config_value) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = ?)",
            key, value, key);
    }

    private void createIndex(String name, String onClause) {
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS " + name + " ON " + onClause);
    }

    private void createIndex(String name, String onClause, String condition) {
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS " + name + " ON " + onClause + " " + condition);
    }
}
