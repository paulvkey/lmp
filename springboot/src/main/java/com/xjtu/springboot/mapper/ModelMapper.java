package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.Model;
import java.util.List;

public interface ModelMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Model row);

    Model selectByPrimaryKey(Long id);

    List<Model> selectAll();

    int updateByPrimaryKey(Model row);
}