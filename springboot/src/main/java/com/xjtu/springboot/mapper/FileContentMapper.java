package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.FileContent;
import java.util.List;

public interface FileContentMapper {
    int deleteByPrimaryKey(Long id);

    int insert(FileContent row);

    FileContent selectByPrimaryKey(Long id);

    List<FileContent> selectAll();

    int updateByPrimaryKey(FileContent row);
}