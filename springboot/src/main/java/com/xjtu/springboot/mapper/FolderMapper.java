package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.Folder;
import java.util.List;

public interface FolderMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Folder row);

    Folder selectByPrimaryKey(Long id);

    List<Folder> selectAll();

    int updateByPrimaryKey(Folder row);
}