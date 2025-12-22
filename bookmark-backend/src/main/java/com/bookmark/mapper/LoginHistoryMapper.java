package com.bookmark.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookmark.entity.LoginHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录历史 Mapper
 */
@Mapper
public interface LoginHistoryMapper extends BaseMapper<LoginHistory> {
}
