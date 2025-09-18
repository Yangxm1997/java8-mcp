package top.yangxm.ai.mcp.org.springframework.ai.tool;

import org.springframework.lang.Nullable;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;
import top.yangxm.ai.mcp.org.springframework.ai.chat.model.ToolContext;
import top.yangxm.ai.mcp.org.springframework.ai.tool.definition.ToolDefinition;
import top.yangxm.ai.mcp.org.springframework.ai.tool.metadata.ToolMetadata;

@SuppressWarnings("unused")
public interface ToolCallback {
    Logger logger = LoggerFactoryHolder.getLogger(ToolCallback.class);

    ToolDefinition getToolDefinition();

    default ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().build();
    }

    String call(String toolInput);

    default String call(String toolInput, @Nullable ToolContext toolContext) {
        if (toolContext != null && !toolContext.getContext().isEmpty()) {
            logger.info("By default the tool context is not used,  "
                    + "override the method 'call(String toolInput, ToolContext toolcon  text)' to support the use of tool context."
                    + "Review the ToolCallback implementation for {}", getToolDefinition().name());
        }
        return call(toolInput);
    }
}
