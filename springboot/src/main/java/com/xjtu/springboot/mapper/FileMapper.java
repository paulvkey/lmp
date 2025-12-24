package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.File;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FileMapper {
    int deleteByPrimaryKey(Long id);

    int insert(File row);

    File selectByPrimaryKey(Long id);

    int updateByPrimaryKey(File row);

    File selectByUserMd5(@Param("userId") Long userId, @Param("sessionId") Long sessionId,
                         @Param("anonymousId") String anonymousId, @Param("fileMd5") String fileMd5);
}