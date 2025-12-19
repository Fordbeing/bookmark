package com.bookmark.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bookmark.entity.User;
import com.bookmark.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = null;

        // 首先尝试按 ID 查询（JWT token 中存储的是用户 ID）
        try {
            Long userId = Long.parseLong(identifier);
            user = userMapper.selectById(userId);
        } catch (NumberFormatException e) {
            // 不是数字，按邮箱查询
        }

        // 如果按 ID 没找到，尝试按邮箱查询
        if (user == null) {
            user = userMapper.selectOne(new QueryWrapper<User>().eq("email", identifier));
        }

        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + identifier);
        }

        return new org.springframework.security.core.userdetails.User(
                String.valueOf(user.getId()), // 使用 ID 作为 username
                user.getPassword() != null ? user.getPassword() : "",
                new ArrayList<>());
    }
}
