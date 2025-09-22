package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransportContext;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.exception.McpError;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCError;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCNotification;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCResponse;

import java.util.Map;

final class DefaultMcpStatelessServerHandler implements McpStatelessServerHandler {
    private static final Logger logger = LoggerFactoryHolder.getLogger(DefaultMcpStatelessServerHandler.class);

    private final Map<String, McpStatelessRequestHandler<?>> requestHandlers;
    private final Map<String, McpStatelessNotificationHandler> notificationHandlers;

    public DefaultMcpStatelessServerHandler(Map<String, McpStatelessRequestHandler<?>> requestHandlers,
                                            Map<String, McpStatelessNotificationHandler> notificationHandlers) {
        this.requestHandlers = requestHandlers;
        this.notificationHandlers = notificationHandlers;
    }

    @Override
    public Mono<JSONRPCResponse> handleRequest(McpTransportContext transportContext, JSONRPCRequest request) {
        McpStatelessRequestHandler<?> requestHandler = this.requestHandlers.get(request.method());
        if (requestHandler == null) {
            return Mono.error(McpError.of("Missing handler for request type: " + request.method()));
        }
        return requestHandler.handle(transportContext, request.params())
                .map(result -> JSONRPCResponse.ofSuccess(request.id(), result))
                .onErrorResume(t -> {
                    if (t instanceof McpError) {
                        McpError mcpError = (McpError) t;
                        if (mcpError.jsonRpcError() != null) {
                            JSONRPCError error = mcpError.jsonRpcError();
                            return Mono.just(JSONRPCResponse.ofError(request.id(), error.code(), error.message()));
                        }
                    }
                    return Mono.just(JSONRPCResponse.ofInternalError(request.id(), t.getMessage()));
                });
    }

    @Override
    public Mono<Void> handleNotification(McpTransportContext transportContext, JSONRPCNotification notification) {
        McpStatelessNotificationHandler notificationHandler = this.notificationHandlers.get(notification.method());
        if (notificationHandler == null) {
            logger.warn("Missing handler for notification type: {}", notification.method());
            return Mono.empty();
        }
        return notificationHandler.handle(transportContext, notification.params());
    }
}
