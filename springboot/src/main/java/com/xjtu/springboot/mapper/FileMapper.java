package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.File;
import java.util.List;

public interface FileMapper {
    int deleteByPrimaryKey(Long id);

    int insert(File row);

    File selectByPrimaryKey(Long id);

    List<File> selectAll();

    int updateByPrimaryKey(File row);
}