package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import reactor.core.publisher.Mono;

public interface McpServerRequestHandler<T> {
    Mono<T> handle(McpAsyncServerExchange exchange, Object params);
}
