package com.example.olineaqspring.service;

import com.example.olineaqspring.entity.SysConfig;
import com.example.olineaqspring.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConfigService {
    private final SysConfigMapper sysConfigMapper;
    private final JdbcTemplate jdbcTemplate;

    @Cacheable(value = "config", key = "'all'")
    public Map<String, String> getAll() {
        List<SysConfig> list = sysConfigMapper.selectList(null);
        Map<String, String> map = new HashMap<>();
        for (SysConfig c : list) {
            map.put(c.getConfigKey(), c.getConfigValue());
        }
        return map;
    }

    @CacheEvict(value = "config", allEntries = true)
    public void update(Map<String, String> configMap) {
        List<Object[]> batchArgs = configMap.entrySet().stream()
                .map(e -> new Object[]{e.getKey(), e.getValue()})
                .toList();
        jdbcTemplate.batchUpdate(
            "INSERT INTO sys_config (config_key, config_value) VALUES (?, ?) " +
            "ON CONFLICT (config_key) DO UPDATE SET config_value = EXCLUDED.config_value",
            batchArgs
        );
    }

    @Cacheable(value = "config", key = "#key")
    public String get(String key) {
        SysConfig config = sysConfigMapper.selectById(key);
        return config == null ? null : config.getConfigValue();
    }
}
