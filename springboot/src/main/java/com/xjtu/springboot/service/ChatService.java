package com.xjtu.springboot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjtu.springboot.dto.chat.ChatDto;
import com.xjtu.springboot.dto.chat.MsgDto;
import com.xjtu.springboot.dto.model.RequestDto;
import com.xjtu.springboot.dto.model.ResponseDto;
import com.xjtu.springboot.exception.CustomException;
import com.xjtu.springboot.mapper.MessageMapper;
import com.xjtu.springboot.mapper.SessionMapper;
import com.xjtu.springboot.mapper.CollectionMapper;
import com.xjtu.springboot.pojo.*;
import com.xjtu.springboot.pojo.common.MsgType;
import com.xjtu.springboot.pojo.common.Role;
import com.xjtu.springboot.pojo.model.Options;
import com.xjtu.springboot.pojo.model.RequestMessage;
import com.xjtu.springboot.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final SessionMapper sessionMapper;
    private final MessageMapper messageMapper;
    private final CollectionMapper collectionMapper;

    private static final String OLLAMA_API_URL = "http://localhost:11434/api/chat";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMinutes(10))
            .build();


    public void chat(ChatDto chatDto,
                     Consumer<ResponseDto> contentConsumer) throws Exception {
        // 构建请求体
        // https://docs.ollama.com/api/chat#response-load-duration
        // https://github.com/ollama/ollama/blob/main/docs/api.md
        RequestDto requestDto = generateRequestDto(chatDto);
        // 转换为JSON
        String requestJson = objectMapper.writeValueAsString(requestDto);
        // 构建请求
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();
        // 处理流式响应
        client.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenApply(HttpResponse::body)
                .thenAccept(lines -> lines.forEach(line -> {
                    try {
                        ResponseDto responseDto = objectMapper.readValue(line, ResponseDto.class);
                        if (Objects.nonNull(responseDto) && responseDto.getMessage() != null
                                && (responseDto.getMessage().getContent() != null || responseDto.getMessage().getThinking() != null)) {
                            contentConsumer.accept(responseDto);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("解析响应异常", e);
                    }
                })).exceptionally(ex -> {
                    // 解析异常根源（ex是CompletionException，其cause才是真正的异常）
                    Throwable rootCause = ex.getCause();
                    String errorMsg;
                    if (rootCause instanceof java.net.ConnectException) {
                        errorMsg = "无法连接到AI服务，请检查Ollama是否已启动（地址：" + OLLAMA_API_URL + "）";
                    } else if (rootCause instanceof java.net.SocketTimeoutException) {
                        errorMsg = "连接AI服务超时，请稍后重试";
                    } else if (rootCause instanceof java.io.IOException) {
                        errorMsg = "与AI服务通信失败：" + rootCause.getMessage();
                    } else {
                        errorMsg = "AI请求处理异常：" + rootCause.getMessage();
                    }
                    // 抛出自定义异常，由上层Controller捕获
                    throw new CustomException(503, errorMsg);
                })
                .join();
    }

    private RequestDto generateRequestDto(ChatDto chatDto) {
        RequestDto requestDto = new RequestDto();
        List<RequestMessage> requestMessageList = new ArrayList<>();
        Options options = new Options();
        if (chatDto.getIsDeepThink() == (byte) 1) {
            requestDto.setThink(true);
            options.setSeed(11223);
            options.setTemperature(0.55);
            options.setTopP(0.96);
        }
        requestDto.setOptions(options);
        for (MsgDto message : chatDto.getMessageList()) {
            // TODO 文件消息要单独处理
            if (Objects.equals(message.getType(), MsgType.FILE.getType())) {
                continue;
            }
            RequestMessage requestMessage = new RequestMessage();
            if (message.getRole() == 1) {
                requestMessage.setRole(Role.USER.getName());
            } else if (message.getRole() == 2) {
                requestMessage.setRole(Role.ASSISTANT.getName());
            }
            requestMessage.setContent(message.getContent());
            requestMessageList.add(requestMessage);
        }
        requestDto.setMessages(requestMessageList);
        return requestDto;
    }

    @Transactional(rollbackFor = Exception.class)
    public ChatDto createSession(ChatDto chatDto) {
        if (chatDto.getNewSession() &&
                (Objects.isNull(chatDto.getSessionId()) || chatDto.getSessionId() == 0)) {
            Session session = generateSession(chatDto);
            if (sessionMapper.insert(session) >= 1) {
                List<Message> messageList = generateMessage(chatDto, session);
                // 这里不对消息进行插入，而是在后面的对话中进行插入
                return generateChatData(chatDto, session, messageList, false);
            } else {
                throw new CustomException(500, "新增对话异常");
            }
        } else {
            throw new CustomException(500, "当前对话不是新对话，无法新增");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ChatDto updateSession(ChatDto chatDto, Boolean selectAllMsg) {
        if (!chatDto.getNewSession() &&
                (Objects.nonNull(chatDto.getSessionId()) && chatDto.getSessionId() > 0)) {
            Long userId = chatDto.getUserId();
            Long sessionId = chatDto.getSessionId();
            Session session = getSession(userId, sessionId);
            if (Objects.nonNull(session)) {
                updateSession(chatDto, session);
                if (sessionMapper.updateByPrimaryKey(session) >= 1) {
                    List<Message> messageList = generateMessage(chatDto, session);
                    for (Message message : messageList) {
                        if (messageMapper.insert(message) < 1) {
                            throw new CustomException(500, "新增对话消息异常");
                        }
                    }
                    if (selectAllMsg) {
                        // 登录用户直接用数据库里面的数据
                        messageList = messageMapper.selectByIds(userId, sessionId);
                    }
                    return generateChatData(chatDto, session, messageList, selectAllMsg);
                } else {
                    throw new CustomException(500, "更新对话异常");
                }
            } else {
                throw new CustomException(500, "获取已有对话异常");
            }
        } else {
            throw new CustomException(500, "当前对话是新对话，无法更新已有对话");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public int updateSession(Session session) {
        return sessionMapper.updateByPrimaryKey(session);
    }

    public Session generateSession(ChatDto chatDto) {
        Session session = new Session();
        session.setUserId(chatDto.getUserId());
        session.setModelId(1L);
        session.setSessionTitle(chatDto.getSessionTitle());
        session.setIsPinned((byte) 0);
        session.setIsCollected((byte) 0);
        session.setIsDeleted((byte) 0);
        session.setCreatedAt(DateUtil.now());
        session.setUpdatedAt(DateUtil.now());
        session.setLastMsgTime(DateUtil.now());
        return session;
    }

    public void updateSession(ChatDto chatDto, Session session) {
        session.setSessionTitle(chatDto.getSessionTitle());
        session.setModelId(chatDto.getModelId());
        session.setIsDeleted(chatDto.getIsDeleted());
        session.setIsCollected(chatDto.getIsCollected());
        session.setUpdatedAt(DateUtil.now());
        session.setLastMsgTime(chatDto.getLastMsgTime());
        session.setIsPinned(chatDto.getIsPinned());
    }

    public List<Message> generateMessage(ChatDto chatDto, Session session) {
        List<Message> messageList = new ArrayList<>();
        chatDto.getMessageList().forEach((msg) -> {
            Message message = new Message();
            message.setUserId(session.getUserId());
            message.setSessionId(session.getId());
            message.setRole(chatDto.getRole());
            message.setThinking(msg.getThinking());
            message.setContent(msg.getContent());
            message.setType(msg.getType().byteValue());
            message.setFileIds(msg.getFileIds());
            message.setTokenCount(msg.getContent().length());
            message.setIsDeepThink(chatDto.getIsDeepThink());
            message.setIsNetworkSearch(chatDto.getIsNetworkSearch());
            message.setSendTime(DateUtil.now());
            messageList.add(message);
        });

        return messageList;
    }

    public ChatDto generateChatData(ChatDto chatDto, Session session,
                                    List<Message> messageList,
                                    Boolean updateMsgList) {
        ChatDto result = new ChatDto();
        result.setIsLogin(true);
        result.setUserId(session.getUserId());
        result.setSessionId(session.getId());
        result.setSessionTitle(session.getSessionTitle());
        result.setModelId(session.getModelId());
        result.setIsPinned(session.getIsPinned());
        result.setIsCollected(session.getIsCollected());
        result.setIsDeleted(session.getIsDeleted());
        result.setCreatedAt(session.getCreatedAt());
        result.setUpdatedAt(session.getUpdatedAt());
        result.setLastMsgTime(session.getLastMsgTime());
        result.setIsDeepThink(chatDto.getIsDeepThink());
        result.setIsNetworkSearch(chatDto.getIsNetworkSearch());
        // 这里只处理消息的基础信息，不处理消息的内容
        Message message = messageList.get(messageList.size() - 1);
        result.setRole(message.getRole());
        result.setSendTime(message.getSendTime());
        result.setTokenCount(message.getTokenCount());
        if (updateMsgList) {
            // 查询出的数据按照时间倒序了，需要从后往前遍历
            result.setMessageList(new ArrayList<>());
            int size = messageList.size();
            for (int i = size - 1; i >= 0; i--) {
                Message chatMsg = messageList.get(i);
                MsgDto msg = MsgDto.builder()
                        .thinking(chatMsg.getThinking())
                        .content(chatMsg.getContent())
                        .type((int) chatMsg.getType())
                        .role((int) chatMsg.getRole())
                        .fileIds(chatMsg.getFileIds())
                        .build();
                result.getMessageList().add(msg);
            }
        }

        return result;
    }

    public Session getSession(Long userId, Long sessionId) {
        Session session = sessionMapper.selectSessionByIds(userId, sessionId);
        if (Objects.nonNull(session)) {
            return session;
        } else {
            throw new CustomException(500, "查询对话详情异常");
        }
    }

    public List<Message> getMessageList(Long userId, Long sessionId) {
        List<Message> messageList = messageMapper.selectByIds(userId, sessionId);
        if (CollectionUtils.isNotEmpty(messageList)) {
            return messageList;
        } else {
            throw new CustomException(500, "查询对话详情异常");
        }
    }

    public List<Session> getSessionList(Long userId) {
        List<Session> sessionList = sessionMapper.selectSessionByUserId(userId);
        if (Objects.nonNull(sessionList)) {
            return sessionList;
        }
        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSession(Long userId, Long sessionId) {
        Session session = getSession(userId, sessionId);
        if (Objects.nonNull(session)) {
            if (sessionMapper.deleteByPrimaryKey(session) >= 1) {
                messageMapper.deleteByIds(userId, sessionId);
                collectionMapper.deleteByIds(userId, sessionId);
                return true;
            } else {
                throw new CustomException(500, "删除对话信息异常");
            }
        } else {
            throw new CustomException(500, "查询待删除对话异常");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Session renameSession(Long userId, Long sessionId, String sessionTitle) {
        Session session = sessionMapper.selectSessionByIds(userId, sessionId);
        if (Objects.nonNull(session)) {
            session.setSessionTitle(sessionTitle);
            if (sessionMapper.updateByPrimaryKey(session) >= 1) {
                return session;
            } else {
                throw new CustomException(500, "重命名对话异常");
            }
        } else {
            throw new CustomException(500, "查询待重命名对话异常");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Session pinnedSession(Long userId, Long sessionId, Integer pinned) {
        Session session = sessionMapper.selectSessionByIds(userId, sessionId);
        if (Objects.nonNull(session)) {
            session.setIsPinned(Byte.valueOf(pinned.toString()));
            if (sessionMapper.updateByPrimaryKey(session) >= 1) {
                return session;
            } else {
                throw new CustomException(500, "置顶/取消置顶对话异常");
            }
        } else {
            throw new CustomException(500, "查询置顶/取消置顶对话异常");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean pauseMsg(Long userId, Long sessionId) {
        List<Message> messageList = messageMapper.selectByIds(userId, sessionId);
        if (CollectionUtils.isNotEmpty(messageList)) {
            int size = messageList.size();
            if (size < 1) {
                return true;
            }
            Message message = messageList.get(size - 1);
            boolean isDeleted = messageMapper.deleteByPrimaryKey(message) > 0;
            if (message.getRole() == (byte) 1) {
                return isDeleted;
            } else if (message.getRole() == (byte) 2 && size >= 2) {
                message = messageList.get(size - 2);
                return isDeleted && messageMapper.deleteByPrimaryKey(message) > 0;
            }
        }
        return true;
    }
}
