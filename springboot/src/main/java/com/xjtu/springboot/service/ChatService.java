package com.xjtu.springboot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjtu.springboot.dto.ChatData;
import com.xjtu.springboot.dto.Msg;
import com.xjtu.springboot.dto.RequestData;
import com.xjtu.springboot.dto.ResponseData;
import com.xjtu.springboot.exception.CustomException;
import com.xjtu.springboot.mapper.ChatMessageMapper;
import com.xjtu.springboot.mapper.ChatSessionMapper;
import com.xjtu.springboot.mapper.UserCollectionMapper;
import com.xjtu.springboot.pojo.*;
import com.xjtu.springboot.util.DateUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
public class ChatService {
    @Autowired
    ChatSessionMapper chatSessionMapper;
    @Autowired
    ChatMessageMapper chatMessageMapper;
    @Autowired
    private UserCollectionMapper userCollectionMapper;

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    public final static String TEXT = "text";
    private final static String FILE = "file";

    private static final String OLLAMA_API_URL = "http://localhost:11434/api/chat";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMinutes(10))
            .build();

    public ChatSession selectSessionById(Long sessionId) {
        ChatSession chatSession = chatSessionMapper.selectByPrimaryKey(sessionId);
        if (Objects.nonNull(chatSession)) {
            return chatSession;
        } else {
            throw new CustomException(500, "查询对话详情异常");
        }
    }

    public List<ChatMessage> selectMessageBySessionId(Long sessionId) {
        List<ChatMessage> chatMessageList = chatMessageMapper.selectBySessionId(sessionId);
        if (CollectionUtils.isNotEmpty(chatMessageList)) {
            return chatMessageList;
        } else {
            throw new CustomException(500, "查询对话详情异常");
        }
    }

    public void chat(ChatData chatData, Long sessionId, Consumer<ResponseData> contentConsumer) throws Exception {
        // 构建请求体
        // https://docs.ollama.com/api/chat#response-load-duration
        // https://github.com/ollama/ollama/blob/main/docs/api.md
        RequestData requestData = genrateRequestData(chatData);
        // 转换为JSON
        String requestJson = objectMapper.writeValueAsString(requestData);
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
                        ResponseData responseData = objectMapper.readValue(line, ResponseData.class);
                        if (Objects.nonNull(responseData) && responseData.getMessage() != null
                                && (responseData.getMessage().getContent() != null || responseData.getMessage().getThinking() != null)) {
                            contentConsumer.accept(responseData);
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

    private RequestData genrateRequestData(ChatData chatData) {
        RequestData requestData = new RequestData();
        List<RequestMessage> requestMessageList = new ArrayList<>();
        for (Msg message : chatData.getMessageList()) {
            // TODO 文件消息要单独处理
            if (message.getType().equals(FILE)) {
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
        requestData.setMessages(requestMessageList);

        Options options = new Options();
        options.setTemperature(0.6);
        requestData.setOptions(options);
        return requestData;
    }

    @Transactional
    public ChatData createSession(ChatData chatData) {
        if (chatData.getNewSession() &&
                (Objects.isNull(chatData.getSessionId()) || chatData.getSessionId() == 0)) {
            ChatSession chatSession = generateSession(chatData);
            if (chatSessionMapper.insert(chatSession) >= 1) {
                List<ChatMessage> chatMessageList = generateMessage(chatData, chatSession);
                // 这里不对消息进行插入，而是在后面的对话中进行插入
                return generateChatData(chatSession, chatMessageList, false);
            } else {
                throw new CustomException(500, "新增对话异常");
            }
        } else {
            throw new CustomException(500, "当前对话不是新对话，无法新增");
        }
    }

    @Transactional
    public ChatData updateSession(ChatData chatData, Boolean selectAllMsg) {
        if (!chatData.getNewSession() &&
                (Objects.nonNull(chatData.getSessionId()) && chatData.getSessionId() > 0)) {
            Long sessionId = chatData.getSessionId();
            ChatSession chatSession = selectSessionById(sessionId);
            if (Objects.nonNull(chatSession)) {
                updateSession(chatData, chatSession);
                if (chatSessionMapper.updateByPrimaryKey(chatSession) >= 1) {
                    List<ChatMessage> chatMessageList = generateMessage(chatData, chatSession);
                    for (ChatMessage chatMessage : chatMessageList) {
                        if (chatMessageMapper.insert(chatMessage) < 1) {
                            throw new CustomException(500, "新增对话消息异常");
                        }
                    }
                    if (selectAllMsg) {
                        // 登录用户直接用数据库里面的数据
                        chatMessageList = chatMessageMapper.selectBySessionId(sessionId);
                    }
                    return generateChatData(chatSession, chatMessageList, selectAllMsg);
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

    @Transactional
    public int updateSession(ChatSession chatSession) {
        return chatSessionMapper.updateByPrimaryKey(chatSession);
    }

    public ChatSession generateSession(ChatData chatData) {
        ChatSession chatSession = new ChatSession();
        chatSession.setUserId(chatData.getUserId());
        chatSession.setAiModelId(1L);
        chatSession.setSessionTitle(chatData.getSessionTitle());
        chatSession.setIsDeleted((byte) 0);
        chatSession.setIsPinned((byte) 0);
        chatSession.setIsCollected((byte) 0);
        chatSession.setCreatedAt(DateUtil.now());
        chatSession.setUpdatedAt(DateUtil.now());
        chatSession.setLastMessageTime(DateUtil.now());
        return chatSession;
    }

    public void updateSession(ChatData chatData, ChatSession chatSession) {
        chatSession.setSessionTitle(chatData.getSessionTitle());
        chatSession.setAiModelId(chatData.getAiModelId());
        chatSession.setIsDeleted(chatData.getIsDeleted());
        chatSession.setIsCollected(chatData.getIsCollected());
        chatSession.setUpdatedAt(DateUtil.now());
        chatSession.setLastMessageTime(chatData.getLastMessageTime());
        chatSession.setIsPinned(chatData.getIsPinned());
    }

    public List<ChatMessage> generateMessage(ChatData chatData, ChatSession chatSession) {
        List<ChatMessage> chatMessageList = new ArrayList<>();
        chatData.getMessageList().forEach((msg) -> {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setUserId(chatSession.getUserId());
            chatMessage.setSessionId(chatSession.getId());
            chatMessage.setMessageType(chatData.getMessageType());
            chatMessage.setMessageThinking(msg.getThinking());
            chatMessage.setMessageContent(msg.getContent());
            chatMessage.setType(msg.getType());
            chatMessage.setFileIds(msg.getFileIds());
            chatMessage.setTokenCount(msg.getContent().length());
            chatMessage.setIsDeepThink(chatData.getIsDeepThink());
            chatMessage.setIsNetworkSearch(chatData.getIsNetworkSearch());
            chatMessage.setSendTime(DateUtil.now());
            chatMessageList.add(chatMessage);
        });

        return chatMessageList;
    }

    public ChatData generateChatData(ChatSession chatSession, List<ChatMessage> chatMessageList,
                                     Boolean updateMsgList) {
        ChatData chatData = new ChatData();
        chatData.setIsLogin(true);
        chatData.setUserId(chatSession.getUserId());
        chatData.setSessionId(chatSession.getId());
        chatData.setSessionTitle(chatSession.getSessionTitle());
        chatData.setAiModelId(chatSession.getAiModelId());
        chatData.setIsPinned(chatSession.getIsPinned());
        chatData.setIsDeleted(chatSession.getIsDeleted());
        chatData.setIsCollected(chatSession.getIsCollected());
        chatData.setCreatedAt(chatSession.getCreatedAt());
        chatData.setUpdatedAt(chatSession.getUpdatedAt());
        chatData.setLastMessageTime(chatSession.getLastMessageTime());
        // 这里只处理消息的基础信息，不处理消息的内容
        ChatMessage chatMessage = chatMessageList.get(0);
        chatData.setMessageType(chatMessage.getMessageType());
        chatData.setSendTime(chatMessage.getSendTime());
        chatData.setTokenCount(chatMessage.getTokenCount());
        chatData.setIsDeepThink(chatMessage.getIsDeepThink());
        chatData.setIsNetworkSearch(chatMessage.getIsNetworkSearch());
        if (updateMsgList) {
            chatData.setMessageList(new ArrayList<>());
            chatMessageList.forEach(chatMsg -> {
                Integer role = chatMsg.getMessageType() == (byte) 1 ? 1 : 2;
                Msg msg = new Msg(chatMsg.getMessageThinking(), chatMsg.getMessageContent(),
                        chatMsg.getType(), role, chatMsg.getFileIds());
                msg.setId(chatMsg.getId());
                chatData.getMessageList().add(msg);
            });
        }

        return chatData;
    }

    public List<ChatSession> selectSessionByUserId(Long userId) {
        List<ChatSession> chatSessionList = chatSessionMapper.selectSessionByUserId(userId);
        if (Objects.nonNull(chatSessionList)) {
            return chatSessionList;
        }
        return null;
    }

    @Transactional
    public boolean deleteSessionById(Long id) {
        ChatSession chatSession = selectSessionById(id);
        if (Objects.nonNull(chatSession)) {
            if (chatSessionMapper.deleteByPrimaryKey(id) >= 1
                    && chatMessageMapper.deleteBySessionId(id) >= 1) {
                if (Objects.nonNull(userCollectionMapper.selectBySessionId(id))) {
                    return userCollectionMapper.deleteBySessionId(id) >= 1;
                }
                return true;
            } else {
                throw new CustomException(500, "删除对话关联信息异常");
            }
        } else {
            throw new CustomException(500, "查询待删除对话异常");
        }
    }

    @Transactional
    public ChatSession renameSessionById(Long id, String sessionTitle) {
        ChatSession chatSession = chatSessionMapper.selectByPrimaryKey(id);
        if (Objects.nonNull(chatSession)) {
            chatSession.setSessionTitle(sessionTitle);
            if (chatSessionMapper.updateByPrimaryKey(chatSession) >= 1) {
                return chatSession;
            } else {
                throw new CustomException(500, "重命名对话异常");
            }
        } else {
            throw new CustomException(500, "查询待重命名对话异常");
        }
    }

    @Transactional
    public ChatSession pinnedSessionById(Long id, Integer pinned) {
        ChatSession chatSession = chatSessionMapper.selectByPrimaryKey(id);
        if (Objects.nonNull(chatSession)) {
            chatSession.setIsPinned(Byte.valueOf(pinned.toString()));
            if (chatSessionMapper.updateByPrimaryKey(chatSession) >= 1) {
                return chatSession;
            } else {
                throw new CustomException(500, "置顶/取消置顶对话异常");
            }
        } else {
            throw new CustomException(500, "查询置顶/取消置顶对话异常");
        }
    }

    @Transactional
    public Boolean pauseMsgBySessionId(Long id, String msgContent) {
        List<ChatMessage> chatMessageList = chatMessageMapper.selectBySessionId(id);
        if (CollectionUtils.isNotEmpty(chatMessageList)) {
            int size = chatMessageList.size();
            ChatMessage chatMessage = chatMessageList.get(size - 1);
            if (chatMessage.getMessageType() == (byte) 1) {
                ChatMessage newChatMessage = generateMsg(chatMessage.getUserId(), id, msgContent, true);
                return chatMessageMapper.insert(newChatMessage) > 0;
            } else if (chatMessage.getMessageType() == (byte) 2) {
                chatMessage.setMessageContent(msgContent);
                chatMessage.setSendTime(DateUtil.now());
                return chatMessageMapper.updateByPrimaryKey(chatMessage) > 0;
            }
        }
        return false;
    }

    public ChatMessage generateMsg(Long userId, Long sessionId, String msgContent, Boolean isSys) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setUserId(userId);
        chatMessage.setSessionId(sessionId);
        if (isSys) {
            chatMessage.setMessageType((byte) 2);
        } else {
            chatMessage.setMessageType((byte) 1);
        }
        chatMessage.setMessageContent(msgContent);
        chatMessage.setType(TEXT);
        chatMessage.setIsDeepThink((byte) 0);
        chatMessage.setIsNetworkSearch((byte) 0);
        chatMessage.setSendTime(DateUtil.now());
        return chatMessage;
    }
}
