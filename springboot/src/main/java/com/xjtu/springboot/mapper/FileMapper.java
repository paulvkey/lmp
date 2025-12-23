package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.File;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FileMapper {
    int deleteByPrimaryKey(File row);

    int insert(File row);

    List<Long> batchInsert(@Param("rows") List<File> rows);

    File selectByPrimaryKey(Long id);

    int updateByPrimaryKey(File row);

    List<File> selectByCondition(Map<String, Object> params);
    long countByCondition(Map<String, Object> params);
    List<File> selectByFolderId(@Param("folderId") Long folderId);

    List<File> selectExpiredChunkFiles(@Param("expireTime") LocalDateTime expireTime);

    File selectByUserMd5(@Param("userId") Long userId, @Param("sessionId") Long sessionId,
                         @Param("anonymousId") String anonymousId, @Param("fileMd5") String fileMd5);
}