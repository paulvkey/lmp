package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.FileFolder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FileFolderMapper {
    int deleteByPrimaryKey(Long id);

    int insert(FileFolder row);

    FileFolder selectByPrimaryKey(Long id);

    List<FileFolder> selectAll();

    int updateByPrimaryKey(FileFolder row);

    FileFolder selectByUserSessionId(@Param("userId") Long userId, @Param("sessionId") Long sessionId);
}