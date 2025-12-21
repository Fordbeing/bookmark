package com.bookmark.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookmark.entity.SharedBookmarks;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SharedBookmarksMapper extends BaseMapper<SharedBookmarks> {
}
