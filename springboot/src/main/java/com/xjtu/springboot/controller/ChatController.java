package com.xjtu.springboot.controller;

import com.xjtu.springboot.common.Result;
import com.xjtu.springboot.component.MessageHolder;
import com.xjtu.springboot.component.ThreadPoolManager;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
public class ChatController {
    @Autowired
    private ChatService chatService;
    @Autowired
    private MessageHolder messageHolder;
    @Autowired
    private ThreadPoolManager threadPoolManager;

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

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
            return createEmptyErrorEmitter("对话消息列表为空");
        }

        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));
        Long sessionId = chatData.getIsLogin() ? chatData.getSessionId() : 0L;
        AtomicBoolean isEmitterCompleted = new AtomicBoolean(false);
        registerEmitterCallbacks(emitter, isEmitterCompleted, chatData, sessionId);

        try {
            if (chatData.getIsLogin()) {
                handleLoginUserChat(chatData, emitter, isEmitterCompleted);
            } else {
                handleNonLoginUserChat(chatData, emitter, isEmitterCompleted);
            }
        } catch (Exception e) {
            handleSyncException(emitter, isEmitterCompleted, chatData, sessionId, e);
        }

        return emitter;
    }

    // ========== 初始化空错误Emitter ==========
    private SseEmitter createEmptyErrorEmitter(String errorMsg) {
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
    private void registerEmitterCallbacks(SseEmitter emitter,
                                          AtomicBoolean isEmitterCompleted,
                                          ChatData chatData,
                                          Long sessionId) {
        // 完成回调
        emitter.onCompletion(() -> {
            isEmitterCompleted.set(true);
            log.debug("SSE连接完成，sessionId: {}, 登录状态: {}", sessionId, chatData.getIsLogin());
            cleanupResources(chatData, "连接完成");
        });

        // 超时回调
        emitter.onTimeout(() -> {
            isEmitterCompleted.set(true);
            log.warn("SSE连接超时，sessionId: {}, 登录状态: {}", sessionId, chatData.getIsLogin());
            try {
                emitter.send(SseEmitter.event()
                        .name(ERROR_EVENT)
                        .data(Result.error(408, "请求超时，请重试")));
            } catch (IOException e) {
                log.error("发送超时事件失败", e);
            } finally {
                cleanupResources(chatData, "超时");
                emitter.complete();
            }
        });

        // 错误回调
        emitter.onError(e -> {
            isEmitterCompleted.set(true);
            log.error("SSE连接异常，sessionId: {}, 登录状态: {}", sessionId, chatData.getIsLogin(), e);
            cleanupResources(chatData, "连接异常");
            emitter.completeWithError(e);
        });
    }

    // ========== 处理登录用户对话逻辑 ==========
    private void handleLoginUserChat(ChatData chatData,
                                     SseEmitter emitter,
                                     AtomicBoolean isEmitterCompleted) throws Exception {
        // 保存用户输入
        chatData.setMessageType((byte) 1);
        ChatData updateData = chatService.updateSession(chatData, true);
        System.out.println(updateData);
        Long loginSessionId = updateData.getSessionId();
        messageHolder.initContentHolder(loginSessionId);
        log.debug("登录用户SSE初始化完成，sessionId: {}", loginSessionId);

        // 异步执行AI流式处理
        executeAiChatAsync(chatData, loginSessionId, emitter, isEmitterCompleted, true, updateData);
    }

    // ========== 处理未登录用户对话逻辑 ==========
    private void handleNonLoginUserChat(ChatData chatData,
                                        SseEmitter emitter,
                                        AtomicBoolean isEmitterCompleted) {
        Long nonLoginSessionId = 0L;
        // 异步执行AI流式处理
        executeAiChatAsync(chatData, nonLoginSessionId, emitter, isEmitterCompleted, false, null);
    }

    // ========== 通用AI异步处理逻辑 ==========
    private void executeAiChatAsync(ChatData chatData,
                                    Long sessionId,
                                    SseEmitter emitter,
                                    AtomicBoolean isEmitterCompleted,
                                    boolean isLogin,
                                    ChatData updateData) {
        CompletableFuture.runAsync(() -> {
            try {
                // 调用AI服务处理流式响应
                chatService.chat(chatData, sessionId, responseData -> {
                    // Emitter已完成则直接返回
                    if (isEmitterCompleted.get()) {
                        log.debug("Emitter已完成，跳过CHUNK事件发送，sessionId: {}", sessionId);
                        return;
                    }
                    // 非空校验
                    if (responseData == null || responseData.getMessage() == null) {
                        log.warn("AI回调数据为空，sessionId: {}", sessionId);
                        return;
                    }

                    // 发送分块消息
                    String thinking = responseData.getMessage().getThinking();
                    String content = responseData.getMessage().getContent();
                    if (StringUtils.isNotEmpty(content) || StringUtils.isNotEmpty(thinking)) {
                        if (isLogin) {
                            if (StringUtils.isNotEmpty(thinking)) {
                                messageHolder.appendContent(sessionId, thinking, true);
                            } else {
                                messageHolder.appendContent(sessionId, content, false);
                            }
                        }
                        sendSseEvent(emitter, isEmitterCompleted, CHUNK_EVENT, responseData);
                    }
                });

                // 发送完成事件
                if (isEmitterCompleted.get()){
                    ChatData finishData = buildFinishChatData(chatData, sessionId, isLogin, updateData);
                    sendSseEvent(emitter, isEmitterCompleted, FINISHED_EVENT, Result.success(finishData));
                }
            } catch (Exception e) {
                // 异步异常处理
                log.error("AI服务执行异常，sessionId: {}", sessionId, e);
                sendSseEvent(emitter, isEmitterCompleted, ERROR_EVENT, Result.error(e.getMessage()));
            } finally {
                // 最终确保Emitter完成，清理资源
                completeEmitterFinally(emitter, isEmitterCompleted, sessionId, isLogin);
            }
        }, threadPoolManager.getThreadPool("chat"));
    }

    // ========== 构建完成事件的ChatData ==========
    private ChatData buildFinishChatData(ChatData originalData,
                                         Long sessionId,
                                         boolean isLogin,
                                         ChatData updateData) {
        ChatData finishData = new ChatData();
        finishData.copyFrom(originalData);
        finishData.setMessageType((byte) 2);
        finishData.setNewSession(false);

        // 登录用户需要组装完整回复
        if (isLogin && updateData != null) {
            String thinking = messageHolder.getCompleteContent(sessionId, true);
            String content = messageHolder.getCompleteContent(sessionId, false);
            Msg msg = new Msg(thinking, content, ChatService.TEXT, 2, "");
            updateData.setMessageList(Collections.singletonList(msg));
            try {
                finishData = chatService.updateSession(updateData, false);
                System.out.println(finishData);
                finishData.setMessageType((byte) 2);
                finishData.setNewSession(false);
            } catch (Exception e) {
                log.error("保存AI回复失败，sessionId: {}", sessionId, e);
            }
        }
        return finishData;
    }

    // ========== 最终完成Emitter并清理资源 ==========
    private void completeEmitterFinally(SseEmitter emitter,
                                        AtomicBoolean isEmitterCompleted,
                                        Long sessionId,
                                        boolean isLogin) {
        if (!isEmitterCompleted.get()) {
            isEmitterCompleted.set(true);
            emitter.complete();
        }
        // 登录用户清理MessageHolder
        if (isLogin) {
            messageHolder.clearContent(sessionId);
        }
    }

    // ========== 处理同步异常 ==========
    private void handleSyncException(SseEmitter emitter,
                                     AtomicBoolean isEmitterCompleted,
                                     ChatData chatData,
                                     Long sessionId,
                                     Exception e) {
        log.error("SSE同步初始化异常，sessionId: {}", sessionId, e);
        isEmitterCompleted.set(true);
        try {
            sendSseEvent(emitter, isEmitterCompleted, ERROR_EVENT, Result.error(e.getMessage()));
        } catch (Exception ex) {
            log.error("发送同步异常事件失败", ex);
        } finally {
            emitter.complete();
            cleanupResources(chatData, "同步初始化异常");
        }
    }

    // ========== 通用SSE事件发送方法 ==========
    private void sendSseEvent(SseEmitter emitter,
                              AtomicBoolean isEmitterCompleted,
                              String eventName,
                              Object data) {
        if (isEmitterCompleted.get()) {
            log.warn("Emitter已完成，跳过发送{}事件", eventName);
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            log.error("发送{}事件失败", eventName, e);
            isEmitterCompleted.set(true);
            emitter.completeWithError(e);
        }
    }

    // 资源清理通用方法
    private void cleanupResources(ChatData chatData, String reason) {
        log.debug("清理资源，原因：{}", reason);
        if (chatData.getIsLogin() && chatData.getSessionId() != null) {
            messageHolder.clearContent(chatData.getSessionId());
        }
    }

}
