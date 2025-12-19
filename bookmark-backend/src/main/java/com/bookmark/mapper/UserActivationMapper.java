package com.bookmark.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookmark.entity.UserActivation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserActivationMapper extends BaseMapper<UserActivation> {

    /**
     * 获取用户有效的额外书签数量
     */
    @Select("SELECT COALESCE(SUM(extra_bookmarks), 0) FROM user_activation " +
            "WHERE user_id = #{userId} AND status = 1 AND expire_time > NOW()")
    Integer getActiveExtraBookmarks(@Param("userId") Long userId);

    /**
     * 获取用户有效的额外分类数量
     */
    @Select("SELECT COALESCE(SUM(extra_categories), 0) FROM user_activation " +
            "WHERE user_id = #{userId} AND status = 1 AND expire_time > NOW()")
    Integer getActiveExtraCategories(@Param("userId") Long userId);
}
