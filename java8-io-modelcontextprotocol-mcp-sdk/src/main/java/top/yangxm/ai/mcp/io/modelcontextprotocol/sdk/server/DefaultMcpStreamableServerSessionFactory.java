package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStreamableServerSession.McpStreamableServerSessionInit;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public final class DefaultMcpStreamableServerSessionFactory implements McpStreamableServerSession.Factory {
    private final Duration requestTimeout;
    private final McpServerInitRequestHandler initRequestHandler;
    private final Map<String, McpServerRequestHandler<?>> requestHandlers;
    private final Map<String, McpServerNotificationHandler> notificationHandlers;

    public DefaultMcpStreamableServerSessionFactory(Duration requestTimeout,
                                                    McpServerInitRequestHandler initRequestHandler,
                                                    Map<String, McpServerRequestHandler<?>> requestHandlers,
                                                    Map<String, McpServerNotificationHandler> notificationHandlers) {
        this.requestTimeout = requestTimeout;
        this.initRequestHandler = initRequestHandler;
        this.requestHandlers = requestHandlers;
        this.notificationHandlers = notificationHandlers;
    }

    @Override
    public McpStreamableServerSessionInit startSession(McpSchema.InitializeRequest initRequest) {
        return new McpStreamableServerSessionInit(
                new McpStreamableServerSession(UUID.randomUUID().toString(),
                        initRequest.capabilities(), initRequest.clientInfo(),
                        requestTimeout, requestHandlers, notificationHandlers),
                this.initRequestHandler.handle(initRequest));
    }
}
