package com.xjtu.springboot.controller;

import com.github.pagehelper.PageInfo;
import com.xjtu.springboot.common.Result;
import com.xjtu.springboot.dto.CollectionData;
import com.xjtu.springboot.pojo.UserCollection;
import com.xjtu.springboot.service.ChatService;
import com.xjtu.springboot.service.UserCollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
public class UserCollectionController {
    @Autowired
    private UserCollectionService userCollectionService;
    @Autowired
    private ChatService chatService;

    @RequestMapping(method = RequestMethod.GET, path = "/collection/user/{userId}")
    public Result selectCollectionByUserId(@PathVariable("userId") Long userId,
                                           @RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "5") int pageSize) {
        if (Objects.nonNull(userId) && userId > 0) {
            PageInfo<UserCollection> userCollections =
                    userCollectionService.getUserCollectionByUserId(userId, pageNum, pageSize);
            return Result.success(userCollections);
        }
        return Result.error("查询收藏异常");
    }

    @RequestMapping(method = RequestMethod.GET, path = "/collection/{id}")
    public Result selectCollectionById(@PathVariable("id") Long id) {
        if (id != null && id > 0) {
            CollectionData collectionData =
                    userCollectionService.getUserCollectionById(id);
            if (Objects.nonNull(collectionData)) {
                return Result.success(collectionData);
            }
        }
        return Result.error("查询收藏详情异常");
    }

    @RequestMapping(method = RequestMethod.PUT, path = "/collection/{sessionId}/add")
    public Result addUserCollection(@PathVariable("sessionId") Long sessionId) {
        if (sessionId != null && sessionId > 0) {
            UserCollection userCollection = userCollectionService.addCollection(sessionId);
            if (Objects.nonNull(userCollection)) {
                CollectionData collectionData = new CollectionData();
                collectionData.setUserCollection(userCollection);
                return Result.success(collectionData);
            } else {
                return Result.error("获取收藏对话异常");
            }
        }
        return Result.error("添加收藏异常");
    }

    @RequestMapping(method = RequestMethod.POST, path = "/collection/{id}/update")
    public Result updateUserCollection(@RequestBody UserCollection userCollection) {
        UserCollection res = userCollectionService.updateCollection(userCollection);
        if (Objects.nonNull(res)) {
            return Result.success(res);
        }
        return Result.error("更新收藏异常");
    }

    @RequestMapping(method = RequestMethod.DELETE, path = "/collection/{id}/delete")
    public Result deleteUserCollection(@PathVariable("id") Long id) {
        if (userCollectionService.deleteCollection(id)) {
            return Result.success();
        }
        return Result.error("删除收藏异常");
    }

    @RequestMapping(method = RequestMethod.DELETE, path = "/collection/session/{sessionId}/delete")
    public Result deleteUserCollectionBySessionId(@PathVariable("sessionId") Long sessionId) {
        if (userCollectionService.deleteCollectionBySessionId(sessionId)) {
            return Result.success();
        }
        return Result.error("删除收藏异常");
    }
}
