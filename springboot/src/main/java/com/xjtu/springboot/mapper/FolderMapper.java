package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.Folder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FolderMapper {
    int deleteByPrimaryKey(Folder row);

    int insert(Folder row);

    Folder selectByPrimaryKey(Folder row);

    int updateByPrimaryKey(Folder row);

    Folder selectByUserSessionId(@Param("userId") Long userId, @Param("sessionId") Long sessionId);

    List<Long> selectSubFolderIds(@Param("folderId") Long folderId);
}