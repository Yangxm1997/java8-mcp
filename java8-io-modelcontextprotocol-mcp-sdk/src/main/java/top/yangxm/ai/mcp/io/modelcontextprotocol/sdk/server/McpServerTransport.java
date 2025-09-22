package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransport;

@SuppressWarnings("unused")
public interface McpServerTransport extends McpTransport {
    String sessionId();
}
