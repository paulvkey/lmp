package com.xjtu.springboot.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.xjtu.springboot.dto.chat.SessionDto;
import com.xjtu.springboot.exception.CustomException;
import com.xjtu.springboot.mapper.CollectionMapper;
import com.xjtu.springboot.pojo.Session;
import com.xjtu.springboot.pojo.Collection;
import com.xjtu.springboot.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class CollectionService {
    @Autowired
    private CollectionMapper collectionMapper;

    @Autowired
    private ChatService chatService;

    public PageInfo<Collection> getCollectionList(Long userId, int pageNum, int pageSize) {
        try (Page<Collection> page = PageHelper.startPage(pageNum, pageSize)
                .doSelectPage(() -> collectionMapper.selectAllByUserId(userId))) {
            return new PageInfo<>(page);
        } catch (Exception e) {
            throw new CustomException(500, "获取收藏列表异常");
        }
    }

    public SessionDto getCollectionData(Long userId, Long collectionId) {
        Collection collection = collectionMapper.selectByIds(userId, collectionId);
        if (Objects.nonNull(collection)) {
            Long sessionId = collection.getSessionId();
            SessionDto sessionDto = new SessionDto();
            sessionDto.setSession(chatService.getSession(userId, sessionId));
            sessionDto.setMessageList(chatService.getMessageList(userId, sessionId));

            return sessionDto;
        } else {
            throw new CustomException(500, "获取收藏详情异常");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Collection addCollection(Long userId, Long sessionId) {
        Session session = chatService.getSession(userId, sessionId);
        if (Objects.nonNull(session)) {
            Collection collection = new Collection();
            collection.setUserId(userId);
            collection.setSessionId(sessionId);
            collection.setSessionTitle(session.getSessionTitle());
            collection.setCreatedAt(DateUtil.now());
            session.setIsCollected((byte) 1);
            if (chatService.updateSession(session) > 0
                    && collectionMapper.insert(collection) > 0) {
                return collection;
            } else {
                throw new CustomException(500, "添加收藏异常");
            }
        } else {
            throw new CustomException(500, "获取收藏对话异常");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Collection updateCollection(Collection collection) {
        if (collectionMapper.updateByPrimaryKey(collection) > 0) {
            return collection;
        } else {
            throw new CustomException(500, "更新收藏异常");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCollection(Long userId, Long collectionId) {
        Collection collection = collectionMapper.selectByIds(userId, collectionId);
        if (Objects.nonNull(collection)) {
            Long sessionId = collection.getSessionId();
            Session session = chatService.getSession(userId, sessionId);
            if (Objects.nonNull(session)) {
                session.setIsCollected((byte) 0);
                if (chatService.updateSession(session) > 0
                        && collectionMapper.deleteByPrimaryKey(collection) > 0) {
                    return true;
                } else {
                    throw new CustomException(500, "取消收藏异常");
                }
            } else {
                throw new CustomException(500, "查询收藏对话详情异常");
            }
        } else {
            throw new CustomException(500, "查询收藏对话异常");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCollectionBySessionId(Long userId, Long sessionId) {
        Session session = chatService.getSession(userId, sessionId);
        if (Objects.nonNull(session)) {
            session.setIsCollected((byte) 0);
            if (chatService.updateSession(session) > 0
                    && collectionMapper.deleteByIds(userId, sessionId) > 0) {
                return true;
            } else {
                throw new CustomException(500, "取消收藏异常");
            }
        } else {
            throw new CustomException(500, "查询收藏对话详情异常");
        }
    }
}
