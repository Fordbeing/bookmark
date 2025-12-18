package com.bookmark.controller;

import com.bookmark.entity.UserSettings;
import com.bookmark.mapper.UserSettingsMapper;
import com.bookmark.service.UserService;
import com.bookmark.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    @Autowired
    private UserSettingsMapper settingsMapper;

    @Autowired
    private UserService userService;

    @GetMapping
    public Result<UserSettings> getSettings() {
        Long userId = userService.getCurrentUser().getId();
        UserSettings settings = settingsMapper.selectOne(new QueryWrapper<UserSettings>().eq("user_id", userId));
        return Result.success(settings);
    }

    @PutMapping
    public Result<Void> updateSettings(@RequestBody Map<String, Object> payload) {
        Long userId = userService.getCurrentUser().getId();
        UserSettings settings = settingsMapper.selectOne(new QueryWrapper<UserSettings>().eq("user_id", userId));

        if (settings == null) {
            settings = new UserSettings();
            settings.setUserId(userId);
        }

        // Update fields
        if (payload.containsKey("theme"))
            settings.setTheme((String) payload.get("theme"));
        if (payload.containsKey("primaryColor"))
            settings.setPrimaryColor((String) payload.get("primaryColor"));
        if (payload.containsKey("secondaryColor"))
            settings.setSecondaryColor((String) payload.get("secondaryColor"));
        if (payload.containsKey("accentColor"))
            settings.setAccentColor((String) payload.get("accentColor"));
        if (payload.containsKey("backgroundColor"))
            settings.setBackgroundColor((String) payload.get("backgroundColor"));
        if (payload.containsKey("sidebarColorFrom"))
            settings.setSidebarColorFrom((String) payload.get("sidebarColorFrom"));
        if (payload.containsKey("sidebarColorTo"))
            settings.setSidebarColorTo((String) payload.get("sidebarColorTo"));
        if (payload.containsKey("displayMode"))
            settings.setDisplayMode((String) payload.get("displayMode"));
        if (payload.containsKey("autoOpenNewTab"))
            settings.setAutoOpenNewTab((Integer) payload.get("autoOpenNewTab"));
        if (payload.containsKey("showStats"))
            settings.setShowStats((Integer) payload.get("showStats"));

        if (settings.getId() == null) {
            settingsMapper.insert(settings);
        } else {
            settingsMapper.updateById(settings);
        }

        return Result.success("设置更新成功", null);
    }
}
