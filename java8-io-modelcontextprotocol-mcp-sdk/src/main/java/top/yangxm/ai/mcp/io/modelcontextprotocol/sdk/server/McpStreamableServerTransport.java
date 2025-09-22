package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransport;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCMessage;

@SuppressWarnings("unused")
public interface McpStreamableServerTransport extends McpTransport {
    String sessionId();

    Mono<Void> sendMessage(JSONRPCMessage message, String messageId);
}