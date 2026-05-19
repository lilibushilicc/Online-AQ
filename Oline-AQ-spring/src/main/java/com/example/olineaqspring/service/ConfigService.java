package com.example.olineaqspring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.olineaqspring.entity.SysConfig;
import com.example.olineaqspring.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConfigService {
    private final SysConfigMapper sysConfigMapper;

    public Map<String, String> getAll() {
        List<SysConfig> list = sysConfigMapper.selectList(null);
        Map<String, String> map = new HashMap<>();
        for (SysConfig c : list) {
            map.put(c.getConfigKey(), c.getConfigValue());
        }
        return map;
    }

    public void update(Map<String, String> configMap) {
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            SysConfig config = sysConfigMapper.selectById(entry.getKey());
            if (config == null) {
                config = new SysConfig();
                config.setConfigKey(entry.getKey());
                config.setConfigValue(entry.getValue());
                sysConfigMapper.insert(config);
            } else {
                config.setConfigValue(entry.getValue());
                sysConfigMapper.updateById(config);
            }
        }
    }

    public String get(String key) {
        SysConfig config = sysConfigMapper.selectById(key);
        return config == null ? null : config.getConfigValue();
    }
}
