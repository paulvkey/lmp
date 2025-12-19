package com.xjtu.springboot.dto;

import com.pgvector.springboot.PGVectorType;
import dev.langchain4j.model.embedding.Embedding;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "rag_embedding")
@Index(name = "idx_embedding", columnList = "embedding", type = IndexType.HASH)
public class RagEmbedding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long fileId; // 关联file表ID
    private String content; // 文档片段内容
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "vector(768)") // 与embedding维度一致
    private float[] embedding; // 向量值
    private LocalDateTime createTime; // 创建时间

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }

    // 转换为LangChain4j的TextSegment
    public dev.langchain4j.data.segment.TextSegment toTextSegment() {
        return dev.langchain4j.data.segment.TextSegment.from(content);
    }

    // 从Embedding转换为float数组
    public static float[] embeddingToFloatArray(Embedding embedding) {
        return embedding.vector().stream().mapToFloat(Float::floatValue).toArray();
    }
}