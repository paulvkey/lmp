package com.xjtu.springboot.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.xjtu.springboot.dto.CollectionData;
import com.xjtu.springboot.dto.SessionData;
import com.xjtu.springboot.exception.CustomException;
import com.xjtu.springboot.mapper.UserCollectionMapper;
import com.xjtu.springboot.pojo.ChatMessage;
import com.xjtu.springboot.pojo.ChatSession;
import com.xjtu.springboot.pojo.UserCollection;
import com.xjtu.springboot.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class UserCollectionService {
    @Autowired
    private UserCollectionMapper userCollectionMapper;

    @Autowired
    private ChatService chatService;

    public PageInfo<UserCollection> getUserCollectionByUserId(Long userId, int pageNum, int pageSize) {
        try (Page<UserCollection> page = PageHelper.startPage(pageNum, pageSize)
                .doSelectPage(() -> userCollectionMapper.selectAllByUserId(userId))) {
            return new PageInfo<>(page);
        } catch (Exception ex) {
            throw new CustomException(500, "查询更多收藏失败");
        }
    }

    public CollectionData getUserCollectionById(Long userId, Long collectionId) {
        UserCollection userCollection = userCollectionMapper.selectByIds(userId, collectionId);
        if (Objects.nonNull(userCollection)) {
            Long sessionId = userCollection.getSessionId();
            ChatSession chatSession = chatService.selectSessionById(userId, sessionId);
            List<ChatMessage> chatMessageList = chatService.selectMessageBySessionId(userId, sessionId);
            SessionData sessionData = new SessionData();
            sessionData.setChatSession(chatSession);
            sessionData.setChatMessageList(chatMessageList);

            CollectionData collectionData = new CollectionData();
            collectionData.setUserCollection(userCollection);
            collectionData.setSessionData(sessionData);
            return collectionData;
        } else {
            throw new CustomException(500, "查询收藏信息异常");
        }
    }

    @Transactional
    public UserCollection addCollection(Long userId, Long sessionId) {
        ChatSession chatSession = chatService.selectSessionById(userId, sessionId);
        if (Objects.nonNull(chatSession)) {
            UserCollection userCollection = new UserCollection();
            userCollection.setUserId(userId);
            userCollection.setSessionId(sessionId);
            userCollection.setSessionTitle(chatSession.getSessionTitle());
            userCollection.setCollectionNote("");
            userCollection.setCollectedAt(DateUtil.now());
            chatSession.setIsCollected((byte) 1);
            if (chatService.updateSession(chatSession) > 0
                    && userCollectionMapper.insert(userCollection) > 0) {
                return userCollection;
            } else {
                throw new CustomException(500, "添加收藏异常");
            }
        } else {
            throw new CustomException(500, "查询收藏对话异常");
        }
    }

    @Transactional
    public UserCollection updateCollection(UserCollection userCollection) {
        if (userCollectionMapper.updateByPrimaryKey(userCollection) > 0) {
            return userCollection;
        } else {
            throw new CustomException(500, "更新收藏异常");
        }
    }

    @Transactional
    public boolean deleteCollection(Long userId, Long collectionId) {
        UserCollection userCollection = userCollectionMapper.selectByIds(userId, collectionId);
        if (Objects.nonNull(userCollection)) {
            Long sessionId = userCollection.getSessionId();
            ChatSession chatSession = chatService.selectSessionById(userId, sessionId);
            if (Objects.nonNull(chatSession)) {
                chatSession.setIsCollected((byte) 0);
                if (chatService.updateSession(chatSession) > 0
                        && userCollectionMapper.deleteByPrimaryKey(userCollection) > 0) {
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

    @Transactional
    public boolean deleteCollectionBySessionId(Long userId, Long sessionId) {
        ChatSession chatSession = chatService.selectSessionById(userId, sessionId);
        if (Objects.nonNull(chatSession)) {
            chatSession.setIsCollected((byte) 0);
            if (chatService.updateSession(chatSession) > 0
                    && userCollectionMapper.deleteByIds(userId, sessionId) > 0) {
                return true;
            } else {
                throw new CustomException(500, "取消收藏异常");
            }
        } else {
            throw new CustomException(500, "查询收藏对话详情异常");
        }
    }
}
