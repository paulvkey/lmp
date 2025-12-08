package com.xjtu.springboot.controller;

import com.xjtu.springboot.common.Result;
import com.xjtu.springboot.component.MessageHolder;
import com.xjtu.springboot.dto.ChatData;
import com.xjtu.springboot.dto.Msg;
import com.xjtu.springboot.dto.SessionData;
import com.xjtu.springboot.pojo.ChatMessage;
import com.xjtu.springboot.pojo.ChatSession;
import com.xjtu.springboot.service.ChatService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@RestController
public class ChatController {
    @Autowired
    private ChatService chatService;
    @Autowired
    private MessageHolder messageHolder;

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private static final String ERROR = "error";
    private static final String CHUNK = "chunk";
    private static final String FINISHED = "finished";

    @RequestMapping(method = RequestMethod.POST, path = "/session/new")
    public Result newSession(@RequestBody ChatData chatData){
        if (CollectionUtils.isEmpty(chatData.getMessageList())) {
            return Result.error("消息列表为空");
        }
        ChatData result = new ChatData();
        try {
            if (chatData.getIsLogin()) {
                result = chatService.createSession(chatData);
                result.setMessageType(chatData.getMessageType());
            } else {
                result.copyFrom(chatData);
            }

            result.setNewSession(false);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
        return Result.success(result);
    }

    @RequestMapping(method = RequestMethod.POST, path = "/session/chat")
    public SseEmitter chat(@RequestBody ChatData chatData) {
        if (CollectionUtils.isEmpty(chatData.getMessageList())) {
            return null;
        }
        // 注册完成回调，清理资源(10分钟)
        SseEmitter emitter = new SseEmitter(600000L);
        emitter.onCompletion(() -> cleanupResources(chatData, "finished"));
        emitter.onTimeout(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .data(Result.error("请求超时，请重试"))
                        .name(ERROR));
            } catch (IOException e) {
                log.error(e.getMessage());
            } finally {
                cleanupResources(chatData, "timeout");
            }
        });

        try {
            if (chatData.getIsLogin()) {
                // 保存输入信息
                chatData.setMessageType((byte) 1);
                ChatData updateData = chatService.updateSession(chatData);
                Long sessionId = updateData.getSessionId();
                messageHolder.initContentHolder(sessionId);
                CompletableFuture.runAsync(() -> {
                    try {
                        // 异步调用模型接口，获取模型的输出
                        chatService.chat(chatData, sessionId, responseData -> {
                            try {
                                String content = responseData.getMessage().getContent();
                                if (StringUtils.isNotEmpty(content)) {
                                    messageHolder.appendContent(sessionId, content);
                                    // 发送分块消息
                                    emitter.send(SseEmitter.event()
                                            .data(responseData)
                                            .name(CHUNK));
                                }
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        });
                    } catch (Exception e) {
                        messageHolder.clearContent(sessionId);
                        emitter.completeWithError(e);
                    }
                });

                updateData.setMessageType((byte) 2);
                updateData.setNewSession(false);
                Msg msg = new Msg(messageHolder.getCompleteContent(sessionId), ChatService.TEXT);
                updateData.setMessageList(Collections.singletonList(msg));
                ChatData result = chatService.updateSession(updateData);
                result.setMessageType((byte) 2);
                result.setNewSession(false);
                emitter.send(SseEmitter.event().data(Result.success(result)).name(FINISHED));
                emitter.complete();
            } else {
                CompletableFuture.runAsync(() -> {
                    try {
                        // 异步调用模型接口，获取模型的输出
                        chatService.chat(chatData, 0L, responseData -> {
                            try {
                                String content = responseData.getMessage().getContent();
                                if (StringUtils.isNotEmpty(content)) {
                                    emitter.send(SseEmitter.event()
                                            .data(responseData)
                                            .name(CHUNK));
                                }
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        });

                        chatData.setMessageType((byte) 2);
                        chatData.setNewSession(false);
                        emitter.send(SseEmitter.event().data(Result.success(chatData)).name(FINISHED));
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                });
            }
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @RequestMapping(method = RequestMethod.GET, path = "session/history/{userId}")
    public Result getSessionByUserId(@PathVariable("userId") Long userId) {
        if (userId != null && userId > 0) {
            List<ChatSession> chatSessionList = chatService.selectSessionByUserId(userId);
            if (Objects.nonNull(chatSessionList)) {
                return Result.success(chatSessionList);
            }
        }
        return Result.error("获取对话历史异常");
    }

    @RequestMapping(method = RequestMethod.GET, path = "session/{id}")
    public Result getSessionById(@PathVariable("id") Long id) {
        if (id != null && id > 0) {
            ChatSession chatSession = chatService.selectSessionById(id);
            if (Objects.nonNull(chatSession)) {
                List<ChatMessage> chatMessageList =
                        chatService.selectMessageBySessionId(chatSession.getId());
                if (CollectionUtils.isNotEmpty(chatMessageList)) {
                    SessionData sessionData = new SessionData();
                    sessionData.setChatSession(chatSession);
                    sessionData.setChatMessageList(chatMessageList);
                    return Result.success(sessionData);
                } else {
                    return Result.error("获取对话信息详情异常");
                }
            } else {
                return Result.error("获取对话信息异常");
            }
        }
        return Result.error("获取对话请求参数异常");
    }

    @RequestMapping(method = RequestMethod.PATCH, path = "/session/{id}/rename")
    public Result renameSession(@PathVariable("id") Long id,
                                @RequestParam("sessionTitle") String sessionTitle) {
        if (id != null && id > 0 && StringUtils.isNotEmpty(sessionTitle)) {
            ChatSession chatSession = chatService.renameSessionById(id, sessionTitle);
            if (Objects.nonNull(chatSession)) {
                return Result.success(chatSession);
            } else {
                return Result.error("重命名对话异常");
            }
        } else {
            return Result.error("获取对话请求参数异常");
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, path = "/session/{id}/delete")
    public Result deleteSession(@PathVariable("id") Long id) {
        if (id != null && id > 0) {
            if (chatService.deleteSessionById(id)) {
                return Result.success();
            } else {
                return Result.error("删除对话异常");
            }
        } else {
            return Result.error("获取对话请求参数异常");
        }
    }

    @RequestMapping(method = RequestMethod.PATCH, path = "/session/{id}/pinned")
    public Result updateSessionPinned(@PathVariable("id") Long id,
                                      @RequestParam("isPinned") Integer isPinned) {
        if (id != null && id > 0) {
            ChatSession chatSession = chatService.pinnedSessionById(id, isPinned);
            if (Objects.nonNull(chatSession)) {
                return Result.success();
            } else {
                return Result.error("获取置顶对话异常");
            }
        } else {
            return Result.error("获取对话请求参数异常");
        }
    }

    // 资源清理通用方法
    private void cleanupResources(ChatData chatData, String reason) {
        if (chatData.getIsLogin() && chatData.getSessionId() != null) {
            messageHolder.clearContent(chatData.getSessionId());
        }
    }
}
