package com.example.olineaqspring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.olineaqspring.config.EmailConstants;
import com.example.olineaqspring.entity.EmailSendLog;
import com.example.olineaqspring.entity.EmailVerification;
import com.example.olineaqspring.entity.SmtpAccount;
import com.example.olineaqspring.mapper.EmailSendLogMapper;
import com.example.olineaqspring.mapper.EmailVerificationMapper;
import com.example.olineaqspring.mapper.SmtpAccountMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final ConfigService configService;
    private final EmailVerificationMapper emailVerificationMapper;
    private final EmailSendLogMapper emailSendLogMapper;
    private final SmtpAccountMapper smtpAccountMapper;

    private static final String CHAR_DIGITS = "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(CHAR_DIGITS.charAt(RANDOM.nextInt(CHAR_DIGITS.length())));
        }
        return sb.toString();
    }

    public void sendVerificationCode(String to, String code) {
        SmtpAccount account = getActiveAccount();
        checkAndResetLimits(account);
        if (account.getHourlyLimit() != null && account.getHourlyLimit() > 0 && account.getHourlySent() >= account.getHourlyLimit()) {
            throw new RuntimeException("当前 SMTP 账号已达到每小时发送上限（" + account.getHourlyLimit() + " 封），请稍后再试或切换账号");
        }
        if (account.getDailyLimit() != null && account.getDailyLimit() > 0 && account.getDailySent() >= account.getDailyLimit()) {
            throw new RuntimeException("当前 SMTP 账号已达到每日发送上限（" + account.getDailyLimit() + " 封），请切换账号或等待重置");
        }

        JavaMailSender sender = createMailSender(account);
        try {
            String systemName = getConfig("smtp.system_name", "Online-AQ 智能在线答题系统");
            String subject = getConfig("smtp.subject", "${systemName} - 邮箱验证")
                    .replace("${systemName}", systemName);
            String template = getConfig("smtp.email_template", null);

            if (template == null || template.isBlank()) {
                template = EmailConstants.DEFAULT_HTML_TEMPLATE;
            }

            String html = template
                    .replace("${code}", code)
                    .replace("${systemName}", systemName);

            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(account.getFromAddress());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);

            incrementCounts(account);
            logSend(account.getId(), to, "register_verify", true);
        } catch (Exception e) {
            logSend(account.getId(), to, "register_verify", false);
            throw new RuntimeException("邮件发送失败：" + e.getMessage());
        }
    }

    private void checkAndResetLimits(SmtpAccount account) {
        LocalDateTime now = LocalDateTime.now();

        if (account.getLastHourlyReset() == null || account.getLastHourlyReset().plusHours(1).isBefore(now)) {
            account.setHourlySent(0);
            account.setLastHourlyReset(now);
        }
        if (account.getLastDailyReset() == null || account.getLastDailyReset().plusDays(1).isBefore(now)) {
            account.setDailySent(0);
            account.setLastDailyReset(now);
        }
    }

    private void incrementCounts(SmtpAccount account) {
        account.setHourlySent(account.getHourlySent() == null ? 1 : account.getHourlySent() + 1);
        account.setDailySent(account.getDailySent() == null ? 1 : account.getDailySent() + 1);
        smtpAccountMapper.updateById(account);
    }

    private String getConfig(String key, String defaultValue) {
        String val = configService.get(key);
        return (val == null || val.isBlank()) ? defaultValue : val;
    }

    public void saveCode(String email, String code) {
        EmailVerification ev = new EmailVerification();
        ev.setEmail(email);
        ev.setCode(code);
        ev.setExpireTime(LocalDateTime.now().plusMinutes(5));
        ev.setUsed(false);
        emailVerificationMapper.insert(ev);
    }

    public boolean verifyCode(String email, String code) {
        LambdaQueryWrapper<EmailVerification> wrapper = new LambdaQueryWrapper<EmailVerification>()
                .eq(EmailVerification::getEmail, email)
                .eq(EmailVerification::getCode, code)
                .eq(EmailVerification::getUsed, false)
                .gt(EmailVerification::getExpireTime, LocalDateTime.now())
                .orderByDesc(EmailVerification::getCreateTime)
                .last("LIMIT 1");
        EmailVerification ev = emailVerificationMapper.selectOne(wrapper);
        if (ev == null) {
            return false;
        }
        ev.setUsed(true);
        emailVerificationMapper.updateById(ev);
        return true;
    }

    public SmtpAccount getActiveAccount() {
        SmtpAccount account = smtpAccountMapper.selectOne(
                new LambdaQueryWrapper<SmtpAccount>().eq(SmtpAccount::getActive, true));
        if (account == null) {
            throw new RuntimeException("未配置 SMTP 账号，请先在系统设置中添加");
        }
        if (Boolean.FALSE.equals(account.getEnabled())) {
            throw new RuntimeException("当前 SMTP 账号已被禁用，请启用或切换到其他账号");
        }
        return account;
    }

    private JavaMailSenderImpl createMailSender(SmtpAccount account) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(account.getHost());
        sender.setPort(account.getPort() != null ? account.getPort() : 587);
        sender.setUsername(account.getUsername());
        sender.setPassword(account.getPassword());

        String timeout = getConfig("smtp.timeout", "30000");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        if ((account.getPort() != null ? account.getPort() : 587) == 465) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }
        props.put("mail.smtp.connectiontimeout", timeout);
        props.put("mail.smtp.timeout", timeout);

        return sender;
    }

    public void testInlineConnection(SmtpAccount account) {
        try {
            if (account.getHost() == null || account.getHost().isBlank()) {
                throw new RuntimeException("请填写 SMTP 服务器");
            }
            if (account.getUsername() == null || account.getUsername().isBlank()) {
                throw new RuntimeException("请填写 SMTP 账号");
            }
            if (account.getPassword() == null || account.getPassword().isBlank()) {
                throw new RuntimeException("请填写 SMTP 密码");
            }
            if (account.getFromAddress() == null || account.getFromAddress().isBlank()) {
                throw new RuntimeException("请填写发件人地址");
            }
            JavaMailSenderImpl sender = createMailSender(account);
            sender.testConnection();
        } catch (Exception e) {
            throw new RuntimeException("SMTP 连接测试失败：" + e.getMessage());
        }
    }

    public boolean testConnection(Integer accountId) {
        try {
            SmtpAccount account;
            if (accountId != null) {
                account = smtpAccountMapper.selectById(accountId);
                if (account == null) {
                    throw new RuntimeException("账号不存在");
                }
            } else {
                account = getActiveAccount();
            }
            JavaMailSenderImpl sender = createMailSender(account);
            if (account.getFromAddress() == null || account.getFromAddress().isBlank()) {
                throw new RuntimeException("请配置发件人地址");
            }
            sender.testConnection();
            return true;
        } catch (Exception e) {
            throw new RuntimeException("SMTP 连接测试失败：" + e.getMessage());
        }
    }

    public List<SmtpAccount> listAccounts() {
        return smtpAccountMapper.selectList(
                new LambdaQueryWrapper<SmtpAccount>().orderByDesc(SmtpAccount::getActive).orderByDesc(SmtpAccount::getId));
    }

    public void saveAccount(SmtpAccount account) {
        if (account.getPort() == null) {
            account.setPort(587);
        }
        if (account.getHourlyLimit() == null) {
            account.setHourlyLimit(100);
        }
        if (account.getDailyLimit() == null) {
            account.setDailyLimit(1000);
        }
        account.setHourlySent(0);
        account.setDailySent(0);
        if (account.getEnabled() == null) {
            account.setEnabled(true);
        }
        boolean firstAccount = smtpAccountMapper.selectCount(null) == 0;
        account.setActive(firstAccount);
        smtpAccountMapper.insert(account);
    }

    public void updateAccount(SmtpAccount account) {
        SmtpAccount existing = smtpAccountMapper.selectById(account.getId());
        if (existing == null) {
            throw new RuntimeException("账号不存在");
        }
        if (account.getHost() != null) existing.setHost(account.getHost());
        if (account.getPort() != null) existing.setPort(account.getPort());
        if (account.getUsername() != null) existing.setUsername(account.getUsername());
        if (account.getPassword() != null) existing.setPassword(account.getPassword());
        if (account.getFromAddress() != null) existing.setFromAddress(account.getFromAddress());
        if (account.getHourlyLimit() != null) existing.setHourlyLimit(account.getHourlyLimit());
        if (account.getDailyLimit() != null) existing.setDailyLimit(account.getDailyLimit());
        smtpAccountMapper.updateById(existing);
    }

    public void deleteAccount(Integer id) {
        SmtpAccount account = smtpAccountMapper.selectById(id);
        if (account == null) return;
        smtpAccountMapper.deleteById(id);
        if (account.getActive()) {
            SmtpAccount first = smtpAccountMapper.selectOne(
                    new LambdaQueryWrapper<SmtpAccount>()
                            .eq(SmtpAccount::getEnabled, true)
                            .last("LIMIT 1"));
            if (first != null) {
                first.setActive(true);
                smtpAccountMapper.updateById(first);
            }
        }
    }

    public void activateAccount(Integer id) {
        SmtpAccount account = smtpAccountMapper.selectById(id);
        if (account == null) {
            throw new RuntimeException("账号不存在");
        }
        if (Boolean.FALSE.equals(account.getEnabled())) {
            throw new RuntimeException("该账号已被禁用，请先启用后再切换");
        }
        smtpAccountMapper.update(null,
                new LambdaUpdateWrapper<SmtpAccount>().set(SmtpAccount::getActive, false));
        account.setActive(true);
        smtpAccountMapper.updateById(account);
    }

    public void toggleEnabled(Integer id) {
        SmtpAccount account = smtpAccountMapper.selectById(id);
        if (account == null) {
            throw new RuntimeException("账号不存在");
        }
        account.setEnabled(Boolean.TRUE.equals(account.getEnabled()) ? false : true);
        smtpAccountMapper.updateById(account);
        // if disabling the active account, deactivate it too
        if (Boolean.FALSE.equals(account.getEnabled()) && Boolean.TRUE.equals(account.getActive())) {
            SmtpAccount first = smtpAccountMapper.selectOne(
                    new LambdaQueryWrapper<SmtpAccount>()
                            .eq(SmtpAccount::getEnabled, true)
                            .ne(SmtpAccount::getId, id)
                            .last("LIMIT 1"));
            if (first != null) {
                first.setActive(true);
                smtpAccountMapper.updateById(first);
            }
            account.setActive(false);
            smtpAccountMapper.updateById(account);
        }
    }

    private void logSend(Integer accountId, String to, String type, boolean success) {
        try {
            EmailSendLog log = new EmailSendLog();
            log.setAccountId(accountId);
            log.setSendTo(to);
            log.setSendType(type);
            log.setSuccess(success);
            emailSendLogMapper.insert(log);
        } catch (Exception ignored) {
        }
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            Object todayCount = emailSendLogMapper.selectCount(
                    new LambdaQueryWrapper<EmailSendLog>()
                            .ge(EmailSendLog::getSendTime, LocalDate.now().atStartOfDay())
            );
            Object weekCount = emailSendLogMapper.selectCount(
                    new LambdaQueryWrapper<EmailSendLog>()
                            .ge(EmailSendLog::getSendTime, LocalDate.now().minusDays(7).atStartOfDay())
            );
            Object totalCount = emailSendLogMapper.selectCount(null);
            EmailSendLog last = emailSendLogMapper.selectOne(
                    new LambdaQueryWrapper<EmailSendLog>()
                            .orderByDesc(EmailSendLog::getSendTime)
                            .last("LIMIT 1")
            );
            stats.put("todayCount", todayCount);
            stats.put("weekCount", weekCount);
            stats.put("totalCount", totalCount);
            stats.put("lastSendTime", last != null ? last.getSendTime() : null);
        } catch (Exception e) {
            stats.put("todayCount", 0);
            stats.put("weekCount", 0);
            stats.put("totalCount", 0);
            stats.put("lastSendTime", null);
        }
        return stats;
    }

    public Map<String, Object> getAccountStats(Integer accountId) {
        Map<String, Object> stats = new HashMap<>();
        try {
            LocalDateTime hourAgo = LocalDateTime.now().minusHours(1);
            LocalDateTime dayStart = LocalDate.now().atStartOfDay();
            Object hourlyCount = emailSendLogMapper.selectCount(
                    new LambdaQueryWrapper<EmailSendLog>()
                            .eq(EmailSendLog::getAccountId, accountId)
                            .ge(EmailSendLog::getSendTime, hourAgo));
            Object dailyCount = emailSendLogMapper.selectCount(
                    new LambdaQueryWrapper<EmailSendLog>()
                            .eq(EmailSendLog::getAccountId, accountId)
                            .ge(EmailSendLog::getSendTime, dayStart));
            stats.put("hourlySend", hourlyCount);
            stats.put("dailySend", dailyCount);
        } catch (Exception e) {
            stats.put("hourlySend", 0);
            stats.put("dailySend", 0);
        }
        return stats;
    }
}
