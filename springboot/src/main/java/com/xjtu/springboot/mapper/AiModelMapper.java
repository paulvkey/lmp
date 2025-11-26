package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.AiModel;
import java.util.List;

public interface AiModelMapper {
    int deleteByPrimaryKey(Long id);

    int insert(AiModel row);

    AiModel selectByPrimaryKey(Long id);

    List<AiModel> selectAll();

    int updateByPrimaryKey(AiModel row);
}