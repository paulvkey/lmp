package com.xjtu.springboot.mapper;

import com.xjtu.springboot.pojo.Collection;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CollectionMapper {
    int deleteByPrimaryKey(Collection row);

    int insert(Collection row);

    int updateByPrimaryKey(Collection row);

    List<Collection> selectAllByUserId(Long userId);

    int deleteByIds(@Param("userId") Long userId, @Param("sessionId") Long sessionId);

    Collection selectByIds(@Param("userId") Long userId, @Param("sessionId") Long sessionId);
}