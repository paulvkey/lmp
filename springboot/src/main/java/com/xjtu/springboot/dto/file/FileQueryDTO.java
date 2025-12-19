package com.xjtu.springboot.dto.file;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class FileQueryDTO {
    /** 登录用户ID */
    private Long userId;
    /** 匿名用户ID */
    private String anonymId;
    /** 会话ID */
    private Long sessionId;
    /** 文件夹ID */
    private Long folderId;
    /** 文件类型（扩展名） */
    private String fileType;
    /** 存储类型 */
    private String storageType;
    /** 上传时间开始 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTimeStart;
    /** 上传时间结束 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTimeEnd;
    /** 页码（默认1） */
    private Integer pageNum = 1;
    /** 每页条数（默认20） */
    private Integer pageSize = 20;
    /** 排序字段（默认uploadAt） */
    private String orderBy = "upload_at";
    /** 排序方向（asc/desc，默认desc） */
    private String orderDir = "desc";
}
