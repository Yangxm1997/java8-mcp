package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransportContext;

@SuppressWarnings("unused")
public interface McpTransportContextExtractor<T> {
    McpTransportContext extract(T request);
}
