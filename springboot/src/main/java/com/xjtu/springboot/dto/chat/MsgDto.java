package com.xjtu.springboot.dto.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class MsgDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String thinking;
    private String content;
    // 1:文本 2:文件 3:图片
    private Integer type;
    // 1:用户 2:AI
    private Integer role;
    private String fileIds;

    private MsgDto(Builder builder) {
        this.thinking = builder.thinking;
        this.content = builder.content;
        this.type = builder.type;
        this.role = builder.role;
        this.fileIds = builder.fileIds;
    }

    public static class Builder {
        private String thinking;
        private String content;
        private Integer type;
        private Integer role;
        private String fileIds;

        public Builder thinking(String thinking) {
            this.thinking = thinking;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder type(Integer type) {
            this.type = type;
            return this;
        }

        public Builder role(Integer role) {
            this.role = role;
            return this;
        }

        public Builder fileIds(String fileIds) {
            this.fileIds = fileIds;
            return this;
        }

        public MsgDto build() {
            return new MsgDto(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}