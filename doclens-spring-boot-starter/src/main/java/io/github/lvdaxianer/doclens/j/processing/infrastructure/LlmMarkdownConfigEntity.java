package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * LLM Markdown 配置 MyBatis-Plus 实体。
 *
 * @author lvdaxianerplus
 * @date 2026-06-09
 */
@Getter
@Setter
@TableName("doclens_llm_markdown_config")
public class LlmMarkdownConfigEntity {

    @TableId("id")
    private String id;
    private String apiType;
    private String url;
    private String model;
    private String credentialRef;
    private boolean credentialConfigured;
    private boolean healthy;
    private String healthMessage;
    private OffsetDateTime lastHealthAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
