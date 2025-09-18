package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import reactor.core.publisher.Mono;

public interface McpServerNotificationHandler {
    Mono<Void> handle(McpAsyncServerExchange exchange, Object params);
}
