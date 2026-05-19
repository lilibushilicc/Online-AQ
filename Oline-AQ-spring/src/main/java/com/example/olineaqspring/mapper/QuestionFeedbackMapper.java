package com.example.olineaqspring.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.olineaqspring.entity.QuestionFeedback;
import com.example.olineaqspring.vo.FeedbackListVO;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface QuestionFeedbackMapper extends BaseMapper<QuestionFeedback> {

    @Select("<script>" +
            "SELECT f.feedback_id, f.question_id, q.question_content, u.real_name AS student_name, " +
            "f.feedback_type, f.description, f.status, f.reject_reason, f.create_time " +
            "FROM question_feedback f " +
            "LEFT JOIN question q ON f.question_id = q.question_id " +
            "LEFT JOIN sys_user u ON f.student_id = u.user_id " +
            "<where>" +
            "<if test='status != null and status != \"\"'> AND f.status = #{status} </if>" +
            "</where>" +
            "ORDER BY f.create_time DESC" +
            "</script>")
    List<FeedbackListVO> selectFeedbackList(String status);

    @Select("SELECT f.feedback_id, f.question_id, q.question_content, u.real_name AS student_name, " +
            "f.feedback_type, f.description, f.status, f.reject_reason, f.create_time " +
            "FROM question_feedback f " +
            "LEFT JOIN question q ON f.question_id = q.question_id " +
            "LEFT JOIN sys_user u ON f.student_id = u.user_id " +
            "WHERE f.feedback_id = #{id}")
    FeedbackListVO selectFeedbackListItem(Integer id);

    @Select("SELECT * FROM question_feedback f WHERE f.student_id = #{studentId}")
    List<QuestionFeedback> selectByStudent(Integer studentId);

    @Select("SELECT COUNT(*) FROM question_feedback WHERE question_id = #{questionId} AND status = 'pending'")
    int countPendingByQuestionId(Integer questionId);

    @Update("UPDATE question_feedback SET status = #{status}, resolve_type = #{resolveType}, " +
            "update_time = CURRENT_TIMESTAMP " +
            "WHERE question_id = #{questionId} AND status = 'pending' AND feedback_id != #{excludeId}")
    int resolveOtherByQuestionId(Integer questionId, String status, String resolveType, Integer excludeId);
}
