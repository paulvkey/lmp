package com.xjtu.springboot.controller;

import com.github.pagehelper.PageInfo;
import com.xjtu.springboot.common.Result;
import com.xjtu.springboot.dto.chat.SessionDto;
import com.xjtu.springboot.pojo.Collection;
import com.xjtu.springboot.service.ChatService;
import com.xjtu.springboot.service.CollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequiredArgsConstructor
public class CollectionController {
    private final CollectionService collectionService;

    @RequestMapping(method = RequestMethod.GET, path = "/collection/user/{userId}")
    public Result selectCollectionByUserId(@PathVariable("userId") Long userId,
                                           @RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "5") int pageSize) {
        if (Objects.nonNull(userId) && userId > 0) {
            PageInfo<Collection> userCollections =
                    collectionService.getCollectionList(userId, pageNum, pageSize);
            return Result.success(userCollections);
        }
        return Result.error("查询收藏异常");
    }

    @RequestMapping(method = RequestMethod.GET, path = "/collection/{userId}")
    public Result selectCollectionById(@PathVariable("userId") Long userId,
                                       @RequestParam("collectionId") Long collectionId) {
        if (userId != null && userId > 0 && collectionId != null && collectionId > 0) {
            SessionDto sessionDto =
                    collectionService.getCollectionData(userId, collectionId);
            if (Objects.nonNull(sessionDto)) {
                return Result.success(sessionDto);
            }
        }
        return Result.error("查询收藏详情异常");
    }

    @RequestMapping(method = RequestMethod.PUT, path = "/collection/{userId}/add")
    public Result addUserCollection(@PathVariable("userId") Long userId,
                                    @RequestParam("sessionId") Long sessionId) {
        if (userId != null && userId > 0 && sessionId != null && sessionId > 0) {
            Collection collection = collectionService.addCollection(userId, sessionId);
            if (Objects.nonNull(collection)) {
                return Result.success(collection);
            } else {
                return Result.error("获取收藏对话异常");
            }
        }
        return Result.error("添加收藏异常");
    }

    @RequestMapping(method = RequestMethod.POST, path = "/collection/update")
    public Result updateUserCollection(@RequestBody Collection collection) {
        Collection res = collectionService.updateCollection(collection);
        if (Objects.nonNull(res)) {
            return Result.success(res);
        }
        return Result.error("更新收藏异常");
    }

    @RequestMapping(method = RequestMethod.DELETE, path = "/collection/{userId}/delete")
    public Result deleteUserCollection(@PathVariable("userId") Long userId,
                                       @RequestParam("collectionId")  Long collectionId) {
        if (collectionService.deleteCollection(userId, collectionId)) {
            return Result.success();
        }
        return Result.error("删除收藏异常");
    }

    @RequestMapping(method = RequestMethod.DELETE, path = "/collection/session/{userId}/delete")
    public Result deleteUserCollectionBySessionId(@PathVariable("userId") Long userId,
                                                  @RequestParam("sessionId") Long sessionId) {
        if (collectionService.deleteCollectionBySessionId(userId, sessionId)) {
            return Result.success();
        }
        return Result.error("删除收藏异常");
    }
}
