package com.xjtu.springboot.controller;

import com.xjtu.springboot.common.Result;
import com.xjtu.springboot.component.storage.MessageHolder;
import com.xjtu.springboot.component.ThreadPoolManager;
import com.xjtu.springboot.dto.chat.ChatDto;
import com.xjtu.springboot.dto.chat.MsgDto;
import com.xjtu.springboot.dto.chat.SessionDto;
import com.xjtu.springboot.pojo.Message;
import com.xjtu.springboot.pojo.Session;
import com.xjtu.springboot.pojo.common.MsgType;
import com.xjtu.springboot.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;
    private final MessageHolder messageHolder;
    private final ThreadPoolManager threadPoolManager;

    private static final String ERROR_EVENT = "error";
    private static final String CHUNK_EVENT = "chunk";
    private static final String FINISHED_EVENT = "finished";

    @RequestMapping(method = RequestMethod.GET, path = "session/history/{userId}")
    public Result getSessionByUserId(@PathVariable("userId") Long userId) {
        if (userId != null && userId > 0) {
            List<Session> sessionList = chatService.getSessionList(userId);
            if (Objects.nonNull(sessionList)) {
                return Result.success(sessionList);
            }
        }
        return Result.error("获取对话历史异常");
    }

    @RequestMapping(method = RequestMethod.GET, path = "session/{userId}")
    public Result getSessionById(@PathVariable("userId") Long userId,
                                 @RequestParam("sessionId") Long sessionId) {
        if (userId != null && userId > 0 && sessionId != null && sessionId > 0) {
            Session session = chatService.getSession(userId, sessionId);
            if (Objects.nonNull(session)) {
                List<Message> messageList =
                        chatService.getMessageList(session.getUserId(), session.getId());
                if (CollectionUtils.isNotEmpty(messageList)) {
                    SessionDto sessionDto = new SessionDto();
                    sessionDto.setSession(session);
                    sessionDto.setMessageList(messageList);
                    return Result.success(sessionDto);
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
            Session session = chatService.renameSession(userId, sessionId, sessionTitle);
            if (Objects.nonNull(session)) {
                return Result.success(session);
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
            if (chatService.deleteSession(userId, sessionId)) {
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
            Session session = chatService.pinnedSession(userId, sessionId, isPinned);
            if (Objects.nonNull(session)) {
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
            if (chatService.pauseMsg(userId, sessionId)) {
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
    public Result newSession(@RequestBody ChatDto chatDto) {
        if (CollectionUtils.isEmpty(chatDto.getMessageList())) {
            return Result.error("消息列表为空");
        }
        ChatDto result = new ChatDto();
        try {
            if (chatDto.getIsLogin() && chatDto.getNewSession()) {
                result = chatService.createSession(chatDto);
                result.setRole(chatDto.getRole());
            } else {
                result.copyFrom(chatDto);
            }
            result.setNewSession(false);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
        return Result.success(result);
    }

    @RequestMapping(method = RequestMethod.POST, path = "/session/chat")
    public SseEmitter chat(@RequestBody ChatDto chatDto) {
        if (CollectionUtils.isEmpty(chatDto.getMessageList())) {
            return emptyErrorEmitter("对话消息列表为空");
        }

        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));
        AtomicBoolean isSseCompleted = new AtomicBoolean(false);
        registerSseCallbacks(emitter, chatDto, isSseCompleted);

        try {
            if (chatDto.getIsLogin()) {
                handleLoginChat(emitter, chatDto, isSseCompleted);
            } else {
                handleNonLoginChat(emitter, chatDto, isSseCompleted);
            }
        } catch (Exception e) {
            handleSyncException(emitter, chatDto, isSseCompleted, e);
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
    private void registerSseCallbacks(SseEmitter emitter, ChatDto chatDto,
                                      AtomicBoolean isSseCompleted) {
        Long userId = chatDto.getUserId();
        Long sessionId = chatDto.getSessionId();
        // 完成回调
        emitter.onCompletion(() -> {
            isSseCompleted.set(true);
            log.debug("SSE连接完成, userId: {}, sessionId: {}", userId, sessionId);
            cleanupResources(chatDto, "连接完成");
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
                cleanupResources(chatDto, "超时");
                emitter.complete();
            }
        });

        // 错误回调
        emitter.onError(e -> {
            isSseCompleted.set(true);
            log.error("SSE连接异常, userId: {}, sessionId: {}", userId, sessionId, e);
            cleanupResources(chatDto, "连接异常");
            emitter.completeWithError(e);
        });
    }

    // ========== 处理登录用户对话逻辑 ==========
    private void handleLoginChat(SseEmitter emitter, ChatDto chatDto,
                                 AtomicBoolean isSseCompleted) {
        // 保存用户输入
        chatDto.setRole((byte) 1);
        ChatDto updateData = chatService.updateSession(chatDto, true);
        Long userId = updateData.getUserId();
        Long sessionId = updateData.getSessionId();
        updateData.setIsLogin(true);
        messageHolder.initHolder(userId, sessionId);
        log.debug("登录用户SSE初始化完成, userId: {}, sessionId: {}", userId, sessionId);

        executeAiChatAsync(chatDto, emitter, isSseCompleted, updateData);
    }

    // ========== 处理未登录用户对话逻辑 ==========
    private void handleNonLoginChat(SseEmitter emitter, ChatDto chatDto,
                                    AtomicBoolean isSseCompleted) {
        chatDto.setIsLogin(false);
        chatDto.setSessionId(0L);
        executeAiChatAsync(chatDto, emitter, isSseCompleted, chatDto);
    }

    // ========== 通用AI异步处理逻辑 ==========
    private void executeAiChatAsync(ChatDto chatDto,
                                    SseEmitter emitter,
                                    AtomicBoolean isSseCompleted,
                                    ChatDto updateData) {
        Boolean isLogin = chatDto.getIsLogin();
        Long userId = chatDto.getUserId();
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
                    ChatDto finishData = buildFinishChatData(chatDto, updateData);
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
    private ChatDto buildFinishChatData(ChatDto chatDto,
                                        ChatDto updateData) {
        Boolean isLogin = updateData.getIsLogin();
        Long userId = updateData.getUserId();
        Long sessionId = updateData.getSessionId();

        ChatDto result = new ChatDto();
        result.copyFrom(chatDto);
        result.setRole((byte) 2);
        result.setNewSession(false);

        // 登录用户需要组装完整回复保存
        if (isLogin && userId > 0 && sessionId > 0) {
            String thinking = messageHolder.getCompleteContent(userId, sessionId, true);
            String content = messageHolder.getCompleteContent(userId, sessionId, false);
            MsgDto msg = MsgDto.builder().thinking(thinking)
                    .content(content)
                    .type(MsgType.TEXT.getType())
                    .role(2)
                    .build();
            updateData.setMessageList(Collections.singletonList(msg));
            updateData.setRole((byte) 2);
            updateData.setNewSession(false);
            try {
                result = chatService.updateSession(updateData, false);
                result.setRole((byte) 2);
                result.setNewSession(false);
            } catch (Exception e) {
                log.error("保存AI回复失败, userId: {}, sessionId: {}", userId, sessionId, e);
            }
        }
        return result;
    }

    // ========== 最终完成Emitter并清理资源 ==========
    private void completeSseFinally(SseEmitter emitter,
                                    ChatDto updateData,
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
    private void handleSyncException(SseEmitter emitter, ChatDto chatDto,
                                     AtomicBoolean isSseCompleted, Exception e) {
        log.error("SSE同步初始化异常, userId: {}, sessionId: {}",
                chatDto.getUserId(), chatDto.getSessionId(), e);
        isSseCompleted.set(true);
        try {
            sendSseEvent(emitter, isSseCompleted, ERROR_EVENT, Result.error(e.getMessage()));
        } catch (Exception ex) {
            log.error("发送同步异常事件失败", ex);
        } finally {
            emitter.complete();
            cleanupResources(chatDto, "同步初始化异常");
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
    private void cleanupResources(ChatDto chatDto, String reason) {
        log.debug("清理资源, 原因：{}", reason);
        Long userId = chatDto.getUserId();
        Long sessionId = chatDto.getSessionId();
        if (chatDto.getIsLogin() && userId > 0 && sessionId > 0) {
            messageHolder.clearContent(userId, sessionId);
        }
    }

}
