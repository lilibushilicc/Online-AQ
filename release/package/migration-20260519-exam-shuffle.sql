-- 为 exam 表添加 shuffle 相关字段（兼容已存在的数据库）
ALTER TABLE exam ADD COLUMN IF NOT EXISTS shuffle_questions BOOLEAN DEFAULT FALSE;
ALTER TABLE exam ADD COLUMN IF NOT EXISTS shuffle_answers BOOLEAN DEFAULT FALSE;
