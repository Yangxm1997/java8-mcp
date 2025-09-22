package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.json.TypeRef;
import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpLoggableSession;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;

@SuppressWarnings("unused")
public final class MissingMcpTransportSession implements McpLoggableSession {
    private final String id;
    private volatile McpSchema.LoggingLevel minLoggingLevel = McpSchema.LoggingLevel.INFO;

    public MissingMcpTransportSession(String sessionId) {
        this.id = sessionId;
    }

    public String id() {
        return id;
    }

    @Override
    public void setMinLoggingLevel(McpSchema.LoggingLevel minLoggingLevel) {
        Assert.notNull(minLoggingLevel, "minLoggingLevel must not be null");
        this.minLoggingLevel = minLoggingLevel;
    }

    @Override
    public boolean isNotificationForLevelAllowed(McpSchema.LoggingLevel loggingLevel) {
        return loggingLevel.level() >= this.minLoggingLevel.level();
    }

    @Override
    public <T> Mono<T> sendRequest(String method, Object requestParams, TypeRef<T> typeRef) {
        return Mono.error(new IllegalStateException("Stream unavailable for session " + this.id));
    }

    @Override
    public Mono<Void> sendNotification(String method, Object params) {
        return Mono.error(new IllegalStateException("Stream unavailable for session " + this.id));
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.empty();
    }

    @Override
    public void close() {
    }
}
