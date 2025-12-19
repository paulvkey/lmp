package com.xjtu.springboot.service;

import cn.hutool.core.util.StrUtil;
import com.example.ragdemo.entity.FileEntity;
import com.example.ragdemo.entity.RagEmbedding;
import com.example.ragdemo.util.DocumentParseUtil;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.bge.BgeEmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.ollama.OllamaLanguageModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG核心服务：文档入库（解析→分割→向量化→存储）+ 检索增强生成
 */
@Service
public class RagService {
    // PGSQL配置
    @Value("${spring.datasource.url}")
    private String pgUrl;
    @Value("${spring.datasource.username}")
    private String pgUser;
    @Value("${spring.datasource.password}")
    private String pgPwd;

    // RAG配置
    @Value("${rag.embedding-dimension}")
    private int embeddingDimension;
    @Value("${rag.chunk-size}")
    private int chunkSize;
    @Value("${rag.chunk-overlap}")
    private int chunkOverlap;
    @Value("${rag.top-k}")
    private int topK;

    // Ollama配置
    @Value("${ollama.base-url}")
    private String ollamaBaseUrl;
    @Value("${ollama.model-name}")
    private String ollamaModelName;

    // 核心组件
    private EmbeddingModel embeddingModel; // 嵌入模型（BGE中文）
    private LanguageModel languageModel;   // LLM（Ollama部署的中文模型）
    private EmbeddingStore<TextSegment> embeddingStore; // 向量存储（PGVector）

    /**
     * 初始化核心组件
     */
    @PostConstruct
    public void init() {
        // 1. 初始化中文嵌入模型（BGE-base-zh，768维度）
        embeddingModel = BgeEmbeddingModel.builder()
                .modelName("bge-base-zh")
                .build();

        // 2. 初始化本地LLM（Ollama + 通义千问7B）
        languageModel = OllamaLanguageModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaModelName)
                .temperature(0.1) // 低温度，回答更精准
                .build();

        // 3. 初始化PGVector向量存储
        embeddingStore = PgVectorEmbeddingStore.builder()
                .jdbcUrl(pgUrl)
                .username(pgUser)
                .password(pgPwd)
                .tableName("rag_embedding") // 与实体表名一致
                .dimension(embeddingDimension)
                .build();
    }

    /**
     * 步骤1：文档入库（上传→解析→分割→向量化→存储）
     */
    public String ingestDocument(MultipartFile file) throws Exception {
        // 1. 基础校验
        String fileName = file.getOriginalFilename();
        String fileMd5 = DocumentParseUtil.getFileMd5(file);
        long fileSize = file.getSize();
        String fileType = FilenameUtils.getExtension(fileName).toLowerCase();

        // 2. 解析文档（提取纯文本）
        String content = DocumentParseUtil.parse(file);
        if (StrUtil.isBlank(content)) {
            return "文档解析失败，无有效内容";
        }

        // 3. 文档分割（中文友好，避免词语截断）
        List<TextSegment> segments = dev.langchain4j.data.document.splitter.TextSplitters
                .recursiveCharacterSplitter(chunkSize, chunkOverlap)
                .split(TextSegment.from(content));

        // 4. 向量化（生成嵌入向量）
        List<Embedding> embeddings = embeddingModel.embedAll(segments.stream()
                .map(TextSegment::text)
                .collect(Collectors.toList()));

        // 5. 存储向量（关联file表）
        embeddingStore.addAll(segments, embeddings);

        // 6. 保存文件基础信息（可选）
        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(fileName);
        fileEntity.setFileType(fileType);
        fileEntity.setFileSize(fileSize);
        fileEntity.setFileMd5(fileMd5);
        // fileRepository.save(fileEntity); // 若需要持久化文件信息，注入repository并保存

        return String.format("文档入库成功！文件名：%s，解析内容长度：%d，分割片段数：%d",
                fileName, content.length(), segments.size());
    }

    /**
     * 步骤2：RAG检索增强生成（问答）
     */
    public String ragAnswer(String question) {
        // 1. 问题向量化
        Embedding questionEmbedding = embeddingModel.embed(question).content();

        // 2. 向量检索（获取最相关的文档片段）
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(questionEmbedding, topK);

        // 3. 构建提示词（结合检索结果）
        StringBuilder prompt = new StringBuilder();
        prompt.append("### 指令：\n");
        prompt.append("基于以下参考文档，用中文简洁、准确地回答问题，仅使用文档中的信息，不要编造内容。\n");
        prompt.append("### 参考文档：\n");
        for (int i = 0; i < matches.size(); i++) {
            prompt.append(String.format("%d. %s\n", i + 1, matches.get(i).embeddedObject().text()));
        }
        prompt.append("### 问题：\n");
        prompt.append(question);

        // 4. 调用LLM生成回答
        return languageModel.generate(prompt.toString());
    }
}
