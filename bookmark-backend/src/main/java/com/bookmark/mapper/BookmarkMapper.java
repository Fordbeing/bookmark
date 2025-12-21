package com.bookmark.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookmark.entity.Bookmark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

@Mapper
public interface BookmarkMapper extends BaseMapper<Bookmark> {

    /**
     * 查询回收站书签（绕过 @TableLogic）
     */
    @Select("SELECT * FROM bookmark WHERE user_id = #{userId} AND status = 0 ORDER BY update_time DESC")
    List<Bookmark> selectTrashByUserId(@Param("userId") Long userId);

    /**
     * 恢复书签（绕过 @TableLogic）
     */
    @Update("UPDATE bookmark SET status = 1 WHERE id = #{id} AND user_id = #{userId} AND status = 0")
    int restoreById(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 永久删除书签（绕过 @TableLogic）
     */
    @Delete("DELETE FROM bookmark WHERE id = #{id} AND user_id = #{userId} AND status = 0")
    int permanentDeleteById(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 清空回收站（绕过 @TableLogic）
     */
    @Delete("DELETE FROM bookmark WHERE user_id = #{userId} AND status = 0")
    int clearTrashByUserId(@Param("userId") Long userId);
}
