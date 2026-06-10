package io.github.lvdaxianer.doclens.j.processing.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lvdaxianer.doclens.j.processing.application.MarkdownPostProcessingRequest;
import java.io.IOException;

/**
 * LLM Markdown 后处理提示词。
 *
 * @author lvdaxianerplus
 * @date 2026-06-10
 */
final class MarkdownPrompt {

    private static final String SYSTEM_PROMPT = """
            你是一个严谨的文档结构化编辑器。你的任务是将 OCR 解析得到的纯文本整理为 Markdown 文档。

            必须遵守：
            1. 只基于输入文本进行格式整理，不得新增、猜测、扩写、总结或删除正文信息。
            2. 保留原文语言、数字、单位、金额、日期、编号、人名、公司名、专业术语和标点含义。
            3. 可以修复明显由 OCR 造成的错误换行、断行、空格和段落粘连，但不得改变原文语义。
            4. Markdown 标题最多只能使用三级：#、##、###。禁止使用 #### 或更深层级。
            5. 只有在文本中明显存在标题、章节、条款层级时才使用标题；无法判断时使用普通段落。
            6. 对明显的列表、编号、条款、表格进行 Markdown 结构化。
            7. 对无法可靠还原成表格的内容，不要强行制作表格，保留为段落或列表。
            8. 如果某一行是否为标题、表格或正文无法判断，请优先保守处理为普通正文。
            9. 如果发现疑似 OCR 错字，不要自行纠正，除非它只是明显的空格、换行或字符粘连问题。
            10. 不要输出解释、备注、处理说明或代码块标记。
            11. 最终只输出 Markdown 内容。
            """;
    private static final String USER_PROMPT_TEMPLATE = """
            请将下面的 OCR 纯文本转换为 Markdown。

            转换目标：
            - 保留完整内容和原始语义
            - 优化段落、标题、列表、表格结构
            - 标题层级最多三级
            - 不进行摘要，不补充输入中不存在的信息
            - 无法确定结构时按普通正文保守处理

            元数据：
            %s

            OCR 文本：
            %s
            """;

    private MarkdownPrompt() {
    }

    /**
     * 读取系统提示词。
     *
     * @return 系统提示词
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    static String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    /**
     * 构建用户提示词。
     *
     * @param objectMapper JSON 映射器
     * @param request Markdown 后处理请求
     * @return 用户提示词
     * @throws IOException 元数据序列化失败
     * @author lvdaxianerplus
     * @date 2026-06-10
     */
    static String userPrompt(ObjectMapper objectMapper, MarkdownPostProcessingRequest request) throws IOException {
        return USER_PROMPT_TEMPLATE.formatted(objectMapper.writeValueAsString(request.metadata()),
                request.ocrText());
    }
}
