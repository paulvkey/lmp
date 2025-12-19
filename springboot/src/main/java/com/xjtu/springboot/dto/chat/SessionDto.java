package com.xjtu.springboot.dto.chat;

import com.xjtu.springboot.pojo.Message;
import com.xjtu.springboot.pojo.Session;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class SessionDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Session session;
    private List<Message> messageList;
}
