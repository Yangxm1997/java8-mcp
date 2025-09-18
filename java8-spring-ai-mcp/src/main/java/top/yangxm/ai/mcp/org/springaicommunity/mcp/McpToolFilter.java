package top.yangxm.ai.mcp.org.springaicommunity.mcp;

import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;

import java.util.function.BiPredicate;

@SuppressWarnings("unused")
public interface McpToolFilter extends BiPredicate<McpConnectionInfo, McpSchema.Tool> {
}
