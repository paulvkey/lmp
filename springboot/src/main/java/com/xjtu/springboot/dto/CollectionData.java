package com.xjtu.springboot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.xjtu.springboot.pojo.UserCollection;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectionData implements Serializable {
    private static final long serialVersionUID = 1L;
    private UserCollection userCollection;
    private SessionData sessionData;
}
