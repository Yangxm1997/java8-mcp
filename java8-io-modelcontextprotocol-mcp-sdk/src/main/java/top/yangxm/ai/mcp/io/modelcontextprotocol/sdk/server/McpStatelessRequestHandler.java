package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransportContext;

public interface McpStatelessRequestHandler<R> {
    Mono<R> handle(McpTransportContext transportContext, Object params);
}