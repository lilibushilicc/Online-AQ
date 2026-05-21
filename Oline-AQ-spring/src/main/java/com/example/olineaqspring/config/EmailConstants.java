package com.example.olineaqspring.config;

public class EmailConstants {

    public static final String DEFAULT_HTML_TEMPLATE = "<div style=\"font-family:'Helvetica Neue',Helvetica,Arial,'PingFang SC','Microsoft YaHei',sans-serif;max-width:480px;margin:0 auto;padding:32px 28px 24px;border:1px solid #d7d0c5;border-radius:10px;background:#f7f5f0;\">"
            + "<div style=\"width:40px;height:3px;background:#16382d;margin-bottom:20px;\"></div>"
            + "<h2 style=\"margin:0 0 16px;font-size:22px;font-weight:700;color:#16201d;font-family:Georgia,'Times New Roman',serif;\">邮箱验证</h2>"
            + "<p style=\"margin:0 0 20px;font-size:14px;line-height:1.8;color:#34423c;\">您好，欢迎注册 <strong style=\"color:#16382d;\">${systemName}</strong>，您的验证码为：</p>"
            + "<div style=\"background:#e3ece7;border-radius:6px;padding:18px;text-align:center;margin:0 0 20px;\">"
            + "<span style=\"font-size:36px;font-weight:bold;letter-spacing:10px;color:#16382d;font-family:Georgia,'Times New Roman',serif;\">${code}</span></div>"
            + "<p style=\"margin:0 0 16px;font-size:12px;color:#8a938e;\">验证码 5 分钟内有效，请勿泄露给他人。</p>"
            + "<hr style=\"border:none;border-top:1px solid #e9e3d9;margin:0 0 14px;\">"
            + "<p style=\"margin:0;font-size:11px;color:#b8b8b8;\">此邮件由系统自动发送，请勿回复。</p></div>";

    private EmailConstants() {}
}
