package top.yangxm.ai.mcp.org.springaicommunity.mcp;

import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;

@SuppressWarnings("unused")
public interface McpToolNamePrefixGenerator {
    String prefixedToolName(McpConnectionInfo mcpConnectionInfo, McpSchema.Tool tool);

    static McpToolNamePrefixGenerator defaultGenerator() {
        return (mcpConnectionIfo, tool) -> McpToolUtils.prefixedToolName(mcpConnectionIfo.clientInfo().name(),
                mcpConnectionIfo.clientInfo().title(), tool.name());
    }

    static McpToolNamePrefixGenerator noPrefix() {
        return (mcpConnectionInfo, tool) -> tool.name();
    }
}
