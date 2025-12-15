package com.xjtu.springboot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Msg implements Serializable {
    private static final long serialVersionUID = 1L;
    private String thinking;
    private String content;
    private String type;
    // 1:用户 2:AI
    private Integer role;
    private String fileIds;

    public Msg() {}

    private Msg(Builder builder) {
        this.thinking = builder.thinking;
        this.content = builder.content;
        this.type = builder.type;
        this.role = builder.role;
        this.fileIds = builder.fileIds;
    }

    public static class Builder {
        private String thinking;
        private String content;
        private String type;
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

        public Builder type(String type) {
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

        public Msg build() {
            return new Msg(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}