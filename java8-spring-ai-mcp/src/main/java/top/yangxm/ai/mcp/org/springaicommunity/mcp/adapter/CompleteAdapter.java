package top.yangxm.ai.mcp.org.springaicommunity.mcp.adapter;

import org.apache.commons.lang3.StringUtils;
import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CompleteReference;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpComplete;

import java.lang.reflect.Method;

@SuppressWarnings("unused")
public final class CompleteAdapter {
    private CompleteAdapter() {
    }

    public static CompleteReference asCompleteReference(McpComplete mcpComplete) {
        Assert.notNull(mcpComplete, "mcpComplete cannot be null");

        String prompt = mcpComplete.prompt();
        String uri = mcpComplete.uri();

        if (StringUtils.isEmpty(prompt) && StringUtils.isEmpty(uri)) {
            throw new IllegalArgumentException("Either prompt or uri must be provided in McpComplete annotation");
        }
        if (StringUtils.isNotEmpty(prompt) && StringUtils.isNotEmpty(uri)) {
            throw new IllegalArgumentException("Only one of prompt or uri can be provided in McpComplete annotation");
        }

        if (StringUtils.isNotEmpty(prompt)) {
            return new McpSchema.PromptReference(prompt);
        } else {
            return new McpSchema.ResourceReference(uri);
        }
    }

    public static CompleteReference asCompleteReference(McpComplete mcpComplete, Method method) {
        Assert.notNull(method, "method cannot be null");
        return asCompleteReference(mcpComplete);
    }
}
