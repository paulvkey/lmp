package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.FileChunkPart;
import java.util.List;

public interface FileChunkPartMapper {
    int deleteByPrimaryKey(Long id);

    int insert(FileChunkPart row);

    FileChunkPart selectByPrimaryKey(Long id);

    List<FileChunkPart> selectAll();

    int updateByPrimaryKey(FileChunkPart row);
}