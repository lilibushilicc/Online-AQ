package com.example.olineaqspring.service;

import com.example.olineaqspring.entity.Question;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionParseServiceTest {

    private final QuestionParseService questionParseService = new QuestionParseService();

    @Test
    void parseUnnumberedSectionedQuestions() {
        String rawText = """
                一、选择题（5 题）
                Spring 框架的核心两大特性是（）
                A. 反射和注解
                B. IOC 控制反转、AOP 面向切面编程
                C. MVC 和 ORM
                D. 依赖注入和序列化
                答案：B
                下列不属于 Spring IOC 容器作用的是（）
                A. 管理 Bean 的创建与销毁
                B. 实现对象依赖注入
                C. 直接操作数据库
                D. 解耦对象之间的依赖关系
                答案：C
                二、简答题（2 题）
                简述什么是 IOC（控制反转），有什么好处？
                答案：
                IOC 即控制反转，是把对象的创建、管理、依赖关系交给 Spring 容器控制。
                好处：解耦代码、便于管理对象、方便测试。
                依赖注入（DI）有哪三种常用方式？
                答案：
                构造器注入、setter 方法注入、字段注解注入。
                简单说是 Spring 事务的传播行为和隔离级别分别是什么作用？
                答案：
                传播行为：控制多个事务方法相互调用时，事务如何传递、是否开启新事务？
                隔离级别：解决并发事务中的脏读、不可重复读、幻读问题。
                """;

        List<Question> questions = questionParseService.parse(rawText, 12, "Spring");

        assertThat(questions).hasSize(5);
        assertThat(questions.get(0).getQuestionContent()).isEqualTo("Spring 框架的核心两大特性是（）");
        assertThat(questions.get(0).getQuestionType()).isEqualTo("single");
        assertThat(questions.get(0).getOptionB()).isEqualTo("IOC 控制反转、AOP 面向切面编程");
        assertThat(questions.get(0).getCorrectAnswer()).isEqualTo("B");
        assertThat(questions.get(2).getQuestionContent()).isEqualTo("简述什么是 IOC（控制反转），有什么好处？");
        assertThat(questions.get(2).getQuestionType()).isEqualTo("short_answer");
        assertThat(questions.get(2).getCorrectAnswer()).contains("控制反转").contains("方便测试");
        assertThat(questions.get(4).getCorrectAnswer()).contains("传播行为").contains("隔离级别");
        assertThat(questions).allSatisfy(question -> {
            assertThat(question.getCategory()).isEqualTo("Spring");
            assertThat(question.getSourceFileId()).isEqualTo(12);
        });
    }
}
