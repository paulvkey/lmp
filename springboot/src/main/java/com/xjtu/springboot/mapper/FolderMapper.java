package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.Folder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FolderMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Folder row);

    int updateByPrimaryKey(Folder row);

    Folder selectByIds(@Param("userId") Long userId, @Param("anonymousId") String anonymousId,
                       @Param("sessionId") Long sessionId);
}