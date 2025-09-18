package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.InitializeRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.InitializeResult;

public interface McpServerInitRequestHandler {
    Mono<InitializeResult> handle(InitializeRequest initializeRequest);
}
