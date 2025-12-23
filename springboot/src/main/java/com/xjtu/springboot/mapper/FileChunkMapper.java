package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.FileChunk;
import java.util.List;

public interface FileChunkMapper {
    int deleteByPrimaryKey(Long id);

    int insert(FileChunk row);

    FileChunk selectByPrimaryKey(Long id);

    List<FileChunk> selectAll();

    int updateByPrimaryKey(FileChunk row);
}