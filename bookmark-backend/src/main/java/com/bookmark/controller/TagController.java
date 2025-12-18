package com.bookmark.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bookmark.entity.Tag;
import com.bookmark.mapper.TagMapper;
import com.bookmark.service.UserService;
import com.bookmark.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tags")
public class TagController {

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private UserService userService;

    /**
     * 获取当前用户的所有标签
     */
    @GetMapping
    public Result<List<Tag>> getAllTags() {
        Long userId = userService.getCurrentUser().getId();
        List<Tag> tags = tagMapper.selectList(
                new QueryWrapper<Tag>()
                        .eq("user_id", userId)
                        .orderByDesc("usage_count"));
        return Result.success(tags);
    }

    /**
     * 创建新标签
     */
    @PostMapping
    public Result<Tag> createTag(@RequestBody Map<String, String> payload) {
        Long userId = userService.getCurrentUser().getId();
        String name = payload.get("name");
        String color = payload.getOrDefault("color", "#6b7280");

        // 检查标签是否已存在
        Tag existing = tagMapper.selectOne(
                new QueryWrapper<Tag>()
                        .eq("user_id", userId)
                        .eq("name", name));
        if (existing != null) {
            return Result.error(400, "标签已存在");
        }

        Tag tag = new Tag();
        tag.setUserId(userId);
        tag.setName(name);
        tag.setColor(color);
        tag.setUsageCount(0);

        tagMapper.insert(tag);
        return Result.success("标签创建成功", tag);
    }

    /**
     * 更新标签
     */
    @PutMapping("/{id}")
    public Result<Tag> updateTag(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Long userId = userService.getCurrentUser().getId();
        Tag tag = tagMapper.selectById(id);

        if (tag == null || !tag.getUserId().equals(userId)) {
            return Result.error(404, "标签不存在");
        }

        if (payload.containsKey("name")) {
            tag.setName(payload.get("name"));
        }
        if (payload.containsKey("color")) {
            tag.setColor(payload.get("color"));
        }

        tagMapper.updateById(tag);
        return Result.success("标签更新成功", tag);
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteTag(@PathVariable Long id) {
        Long userId = userService.getCurrentUser().getId();
        Tag tag = tagMapper.selectById(id);

        if (tag == null || !tag.getUserId().equals(userId)) {
            return Result.error(404, "标签不存在");
        }

        tagMapper.deleteById(id);
        return Result.success("标签删除成功", null);
    }

    /**
     * 增加标签使用次数
     */
    @PostMapping("/{id}/increment")
    public Result<Void> incrementUsage(@PathVariable Long id) {
        Long userId = userService.getCurrentUser().getId();
        Tag tag = tagMapper.selectById(id);

        if (tag == null || !tag.getUserId().equals(userId)) {
            return Result.error(404, "标签不存在");
        }

        tag.setUsageCount(tag.getUsageCount() + 1);
        tagMapper.updateById(tag);
        return Result.success("使用次数已更新", null);
    }
}
