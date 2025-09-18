package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransportContext;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ClientCapabilities;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CreateMessageRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CreateMessageResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ElicitResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Implementation;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ListRootsResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.LoggingMessageNotification;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ProgressNotification;

@SuppressWarnings("unused")
public class McpSyncServerExchange {
    private final McpAsyncServerExchange exchange;

    public McpSyncServerExchange(McpAsyncServerExchange exchange) {
        this.exchange = exchange;
    }

    public String sessionId() {
        return this.exchange.sessionId();
    }

    public ClientCapabilities clientCapabilities() {
        return this.exchange.clientCapabilities();
    }

    public Implementation clientInfo() {
        return this.exchange.clientInfo();
    }

    public McpTransportContext transportContext() {
        return this.exchange.transportContext();
    }

    public CreateMessageResult createMessage(CreateMessageRequest createMessageRequest) {
        return this.exchange.createMessage(createMessageRequest).block();
    }

    public ElicitResult createElicitation(McpSchema.ElicitRequest elicitRequest) {
        return this.exchange.createElicitation(elicitRequest).block();
    }

    public ListRootsResult listRoots() {
        return this.exchange.listRoots().block();
    }

    public ListRootsResult listRoots(String cursor) {
        return this.exchange.listRoots(cursor).block();
    }

    public void loggingNotification(LoggingMessageNotification loggingMessageNotification) {
        this.exchange.loggingNotification(loggingMessageNotification).block();
    }

    public void progressNotification(ProgressNotification progressNotification) {
        this.exchange.progressNotification(progressNotification).block();
    }

    public Object ping() {
        return this.exchange.ping().block();
    }
}
