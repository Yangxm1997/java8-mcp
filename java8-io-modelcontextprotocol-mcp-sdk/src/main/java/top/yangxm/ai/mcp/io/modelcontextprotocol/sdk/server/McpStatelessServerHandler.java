package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransportContext;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCNotification;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCResponse;

@SuppressWarnings("unused")
public interface McpStatelessServerHandler {
    Mono<JSONRPCResponse> handleRequest(McpTransportContext transportContext, JSONRPCRequest request);

    Mono<Void> handleNotification(McpTransportContext transportContext, JSONRPCNotification notification);
}
