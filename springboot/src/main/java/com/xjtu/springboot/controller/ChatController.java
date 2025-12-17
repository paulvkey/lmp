package com.xjtu.springboot.controller;

import com.xjtu.springboot.common.Result;
import com.xjtu.springboot.component.storage.MessageHolder;
import com.xjtu.springboot.component.ThreadPoolManager;
import com.xjtu.springboot.dto.ChatData;
import com.xjtu.springboot.dto.Msg;
import com.xjtu.springboot.dto.SessionData;
import com.xjtu.springboot.pojo.ChatMessage;
import com.xjtu.springboot.pojo.ChatSession;
import com.xjtu.springboot.service.ChatService;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RestController
public class ChatController {
    @Autowired
    private ChatService chatService;
    @Autowired
    private MessageHolder messageHolder;
    @Autowired
    private ThreadPoolManager threadPoolManager;

    private static final String ERROR_EVENT = "error";
    private static final String CHUNK_EVENT = "chunk";
    private static final String FINISHED_EVENT = "finished";

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

    @RequestMapping(method = RequestMethod.GET, path = "session/{userId}")
    public Result getSessionById(@PathVariable("userId") Long userId,
                                 @RequestParam("sessionId") Long sessionId) {
        if (userId != null && userId > 0 && sessionId != null && sessionId > 0) {
            ChatSession chatSession = chatService.selectSessionById(userId, sessionId);
            if (Objects.nonNull(chatSession)) {
                List<ChatMessage> chatMessageList =
                        chatService.selectMessageBySessionId(chatSession.getUserId(), chatSession.getId());
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

    @RequestMapping(method = RequestMethod.PATCH, path = "/session/{userId}/rename")
    public Result renameSession(@PathVariable("userId") Long userId,
                                @RequestParam("sessionId") Long sessionId,
                                @RequestParam("sessionTitle") String sessionTitle) {
        if (userId != null && userId > 0 &&
                sessionId != null && sessionId > 0 &&
                StringUtils.isNotEmpty(sessionTitle)) {
            ChatSession chatSession = chatService.renameSessionById(userId, sessionId, sessionTitle);
            if (Objects.nonNull(chatSession)) {
                return Result.success(chatSession);
            } else {
                return Result.error("重命名对话异常");
            }
        } else {
            return Result.error("获取对话请求参数异常");
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, path = "/session/{userId}/delete")
    public Result deleteSession(@PathVariable("userId") Long userId,
                                @RequestParam("sessionId") Long sessionId) {
        if (userId != null && userId > 0 && sessionId != null && sessionId > 0) {
            if (chatService.deleteSessionById(userId, sessionId)) {
                messageHolder.clearContent(userId, sessionId);
                return Result.success();
            } else {
                return Result.error("删除对话异常");
            }
        } else {
            return Result.error("获取对话请求参数异常");
        }
    }

    @RequestMapping(method = RequestMethod.PATCH, path = "/session/{userId}/pinned")
    public Result updateSessionPinned(@PathVariable("userId") Long userId,
                                      @RequestParam("sessionId") Long sessionId,
                                      @RequestParam("isPinned") Integer isPinned) {
        if (userId != null && userId > 0 && sessionId != null && sessionId > 0) {
            ChatSession chatSession = chatService.pinnedSessionById(userId, sessionId, isPinned);
            if (Objects.nonNull(chatSession)) {
                return Result.success();
            } else {
                return Result.error("获取置顶对话异常");
            }
        } else {
            return Result.error("获取对话请求参数异常");
        }
    }

    @RequestMapping(method = RequestMethod.PATCH, path = "/session/{userId}/pause")
    public Result pauseSessionMsg(@PathVariable("userId") Long userId,
                                  @RequestParam("sessionId") Long sessionId) {
        if (userId != null && userId > 0 && sessionId != null && sessionId > 0) {
            if (chatService.pauseMsgBySessionId(userId, sessionId)) {
                messageHolder.clearContent(userId, sessionId);
                return Result.success();
            } else {
                return Result.error("终止对话消息异常");
            }
        } else {
            return Result.error("获取对话请求参数异常");
        }
    }

    @RequestMapping(method = RequestMethod.POST, path = "/session/chat/new")
    public Result newSession(@RequestBody ChatData chatData) {
        if (CollectionUtils.isEmpty(chatData.getMessageList())) {
            return Result.error("消息列表为空");
        }
        ChatData result = new ChatData();
        try {
            if (chatData.getIsLogin() && chatData.getNewSession()) {
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
            return emptyErrorEmitter("对话消息列表为空");
        }

        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));
        AtomicBoolean isSseCompleted = new AtomicBoolean(false);
        registerSseCallbacks(emitter, chatData, isSseCompleted);

        try {
            if (chatData.getIsLogin()) {
                handleLoginChat(emitter, chatData, isSseCompleted);
            } else {
                handleNonLoginChat(emitter, chatData, isSseCompleted);
            }
        } catch (Exception e) {
            handleSyncException(emitter, chatData, isSseCompleted, e);
        }

        return emitter;
    }

    // ========== 初始化空错误Emitter ==========
    private SseEmitter emptyErrorEmitter(String errorMsg) {
        SseEmitter emptyEmitter = new SseEmitter(0L);
        try {
            emptyEmitter.send(SseEmitter.event()
                    .name(ERROR_EVENT)
                    .data(Result.error(errorMsg)));
        } catch (IOException e) {
            log.warn("发送空Emitter错误消息失败: {}", errorMsg, e);
        } finally {
            emptyEmitter.complete();
        }
        return emptyEmitter;
    }

    // ========== 注册Emitter回调 ==========
    private void registerSseCallbacks(SseEmitter emitter, ChatData chatData,
                                      AtomicBoolean isSseCompleted) {
        Long userId = chatData.getUserId();
        Long sessionId = chatData.getSessionId();
        // 完成回调
        emitter.onCompletion(() -> {
            isSseCompleted.set(true);
            log.debug("SSE连接完成, userId: {}, sessionId: {}", userId, sessionId);
            cleanupResources(chatData, "连接完成");
        });

        // 超时回调
        emitter.onTimeout(() -> {
            isSseCompleted.set(true);
            log.warn("SSE连接超时, userId: {}, sessionId: {}", userId, sessionId);
            try {
                emitter.send(SseEmitter.event()
                        .name(ERROR_EVENT)
                        .data(Result.error(408, "请求超时, 请重试")));
            } catch (IOException e) {
                log.error("发送超时事件失败", e);
            } finally {
                cleanupResources(chatData, "超时");
                emitter.complete();
            }
        });

        // 错误回调
        emitter.onError(e -> {
            isSseCompleted.set(true);
            log.error("SSE连接异常, userId: {}, sessionId: {}", userId, sessionId, e);
            cleanupResources(chatData, "连接异常");
            emitter.completeWithError(e);
        });
    }

    // ========== 处理登录用户对话逻辑 ==========
    private void handleLoginChat(SseEmitter emitter, ChatData chatData,
                                 AtomicBoolean isSseCompleted) {
        // 保存用户输入
        chatData.setMessageType((byte) 1);
        ChatData updateData = chatService.updateSession(chatData, true);
        Long userId = updateData.getUserId();
        Long sessionId = updateData.getSessionId();
        updateData.setIsLogin(true);
        messageHolder.initHolder(userId, sessionId);
        log.debug("登录用户SSE初始化完成, userId: {}, sessionId: {}", userId, sessionId);

        executeAiChatAsync(chatData, emitter, isSseCompleted, updateData);
    }

    // ========== 处理未登录用户对话逻辑 ==========
    private void handleNonLoginChat(SseEmitter emitter, ChatData chatData,
                                    AtomicBoolean isSseCompleted) {
        chatData.setIsLogin(false);
        chatData.setSessionId(0L);
        executeAiChatAsync(chatData, emitter, isSseCompleted, chatData);
    }

    // ========== 通用AI异步处理逻辑 ==========
    private void executeAiChatAsync(ChatData chatData,
                                    SseEmitter emitter,
                                    AtomicBoolean isSseCompleted,
                                    ChatData updateData) {
        Boolean isLogin = chatData.getIsLogin();
        Long userId = chatData.getUserId();
        Long sessionId = updateData.getSessionId();
        CompletableFuture.runAsync(() -> {
            try {
                // 调用AI服务处理流式响应
                chatService.chat(updateData, responseData -> {
                    // Emitter已完成则直接返回
                    if (isSseCompleted.get()) {
                        log.debug("Emitter已完成, 跳过CHUNK事件发送, userId: {}, sessionId: {}", userId, sessionId);
                        return;
                    }
                    if (responseData == null || responseData.getMessage() == null) {
                        log.warn("AI回调数据为空, userId: {}, sessionId: {}", userId, sessionId);
                        return;
                    }

                    // 发送分块消息
                    String thinking = responseData.getMessage().getThinking();
                    String content = responseData.getMessage().getContent();
                    if (StringUtils.isNotEmpty(content) || StringUtils.isNotEmpty(thinking)) {
                        if (isLogin) {
                            if (StringUtils.isNotEmpty(thinking)) {
                                messageHolder.appendContent(userId, sessionId, thinking, true);
                            } else if (StringUtils.isNotEmpty(content)) {
                                messageHolder.appendContent(userId, sessionId, content, false);
                            }
                        }
                        sendSseEvent(emitter, isSseCompleted, CHUNK_EVENT, responseData);
                    }
                });

                // 发送完成事件
                if (!isSseCompleted.get()) {
                    ChatData finishData = buildFinishChatData(chatData, updateData);
                    sendSseEvent(emitter, isSseCompleted, FINISHED_EVENT, Result.success(finishData));
                }
            } catch (Exception e) {
                // 异步异常处理
                log.error("AI服务执行异常, userId: {}, sessionId: {}", userId, sessionId, e);
                sendSseEvent(emitter, isSseCompleted, ERROR_EVENT, Result.error(e.getMessage()));
            } finally {
                // 最终确保Emitter完成, 清理资源
                completeSseFinally(emitter, updateData, isSseCompleted);
            }
        }, threadPoolManager.getThreadPool("chat"));
    }

    // ========== 构建完成事件的ChatData ==========
    private ChatData buildFinishChatData(ChatData chatData,
                                         ChatData updateData) {
        Boolean isLogin = updateData.getIsLogin();
        Long userId = updateData.getUserId();
        Long sessionId = updateData.getSessionId();

        ChatData result = new ChatData();
        result.copyFrom(chatData);
        result.setMessageType((byte) 2);
        result.setNewSession(false);

        // 登录用户需要组装完整回复保存
        if (isLogin && userId > 0 && sessionId > 0) {
            String thinking = messageHolder.getCompleteContent(userId, sessionId, true);
            String content = messageHolder.getCompleteContent(userId, sessionId, false);
            Msg msg = Msg.builder().thinking(thinking)
                    .content(content)
                    .type(ChatService.TEXT)
                    .role(2)
                    .build();
            updateData.setMessageList(Collections.singletonList(msg));
            updateData.setMessageType((byte) 2);
            updateData.setNewSession(false);
            try {
                result = chatService.updateSession(updateData, false);
                result.setMessageType((byte) 2);
                result.setNewSession(false);
            } catch (Exception e) {
                log.error("保存AI回复失败, userId: {}, sessionId: {}", userId, sessionId, e);
            }
        }
        return result;
    }

    // ========== 最终完成Emitter并清理资源 ==========
    private void completeSseFinally(SseEmitter emitter,
                                    ChatData updateData,
                                    AtomicBoolean isSseCompleted) {
        if (!isSseCompleted.get()) {
            isSseCompleted.set(true);
            emitter.complete();
        }
        // 登录用户清理MessageHolder
        Long userId = updateData.getUserId();
        Long sessionId = updateData.getSessionId();
        if (updateData.getIsLogin() && userId > 0 && sessionId > 0) {
            messageHolder.clearContent(userId, sessionId);
        }
    }

    // ========== 处理同步异常 ==========
    private void handleSyncException(SseEmitter emitter, ChatData chatData,
                                     AtomicBoolean isSseCompleted, Exception e) {
        log.error("SSE同步初始化异常, userId: {}, sessionId: {}",
                chatData.getUserId(), chatData.getSessionId(), e);
        isSseCompleted.set(true);
        try {
            sendSseEvent(emitter, isSseCompleted, ERROR_EVENT, Result.error(e.getMessage()));
        } catch (Exception ex) {
            log.error("发送同步异常事件失败", ex);
        } finally {
            emitter.complete();
            cleanupResources(chatData, "同步初始化异常");
        }
    }

    // ========== 通用SSE事件发送方法 ==========
    private void sendSseEvent(SseEmitter emitter,
                              AtomicBoolean isSseCompleted,
                              String eventName,
                              Object data) {
        if (isSseCompleted.get()) {
            log.warn("Emitter已完成, 跳过发送{}事件", eventName);
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            log.error("发送{}事件失败", eventName, e);
            isSseCompleted.set(true);
            emitter.completeWithError(e);
        }
    }

    // 资源清理通用方法
    private void cleanupResources(ChatData chatData, String reason) {
        log.debug("清理资源, 原因：{}", reason);
        Long userId = chatData.getUserId();
        Long sessionId = chatData.getSessionId();
        if (chatData.getIsLogin() && userId > 0 && sessionId > 0) {
            messageHolder.clearContent(userId, sessionId);
        }
    }

}
