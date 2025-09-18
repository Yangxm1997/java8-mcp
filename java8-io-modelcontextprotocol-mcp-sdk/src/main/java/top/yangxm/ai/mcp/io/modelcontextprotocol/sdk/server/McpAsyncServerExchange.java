package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.json.TypeRef;
import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpLoggableSession;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransportContext;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.exception.McpError;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ClientCapabilities;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CreateMessageRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CreateMessageResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ElicitRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ElicitResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Implementation;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ListRootsResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.LoggingLevel;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.LoggingMessageNotification;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ProgressNotification;

import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings("unused")
public class McpAsyncServerExchange {
    private final String sessionId;
    private final McpLoggableSession session;
    private final ClientCapabilities clientCapabilities;
    private final Implementation clientInfo;
    private final McpTransportContext transportContext;

    private static final TypeRef<CreateMessageResult> CREATE_MESSAGE_RESULT_TYPE_REF = new TypeRef<CreateMessageResult>() {
    };
    private static final TypeRef<ListRootsResult> LIST_ROOTS_RESULT_TYPE_REF = new TypeRef<ListRootsResult>() {
    };
    private static final TypeRef<ElicitResult> ELICITATION_RESULT_TYPE_REF = new TypeRef<ElicitResult>() {
    };
    private static final TypeRef<Object> OBJECT_TYPE_REF = new TypeRef<Object>() {
    };

    public McpAsyncServerExchange(String sessionId, McpLoggableSession session,
                                  ClientCapabilities clientCapabilities, Implementation clientInfo,
                                  McpTransportContext transportContext) {
        this.sessionId = sessionId;
        this.session = session;
        this.clientCapabilities = clientCapabilities;
        this.clientInfo = clientInfo;
        this.transportContext = transportContext;
    }

    public String sessionId() {
        return this.sessionId;
    }

    public ClientCapabilities clientCapabilities() {
        return this.clientCapabilities;
    }

    public Implementation clientInfo() {
        return this.clientInfo;
    }

    public McpTransportContext transportContext() {
        return this.transportContext;
    }

    public Mono<CreateMessageResult> createMessage(CreateMessageRequest createMessageRequest) {
        if (this.clientCapabilities == null) {
            return Mono.error(McpError.of("Client must be initialized. Call the initialize method first!"));
        }
        if (this.clientCapabilities.sampling() == null) {
            return Mono.error(McpError.of("Client must be configured with sampling capabilities"));
        }
        return this.session.sendRequest(McpSchema.METHOD_SAMPLING_CREATE_MESSAGE, createMessageRequest, CREATE_MESSAGE_RESULT_TYPE_REF);
    }

    public Mono<ElicitResult> createElicitation(ElicitRequest elicitRequest) {
        if (this.clientCapabilities == null) {
            return Mono.error(McpError.of("Client must be initialized. Call the initialize method first!"));
        }
        if (this.clientCapabilities.elicitation() == null) {
            return Mono.error(McpError.of("Client must be configured with elicitation capabilities"));
        }
        return this.session.sendRequest(McpSchema.METHOD_ELICITATION_CREATE, elicitRequest, ELICITATION_RESULT_TYPE_REF);
    }

    public Mono<ListRootsResult> listRoots() {
        return this.listRoots(McpSchema.FIRST_PAGE)
                .expand(result -> (result.nextCursor() != null) ?
                        this.listRoots(result.nextCursor()) : Mono.empty())
                .reduce(new ListRootsResult(new ArrayList<>(), null),
                        (allRootsResult, result) -> {
                            allRootsResult.roots().addAll(result.roots());
                            return allRootsResult;
                        })
                .map(result -> new ListRootsResult(Collections.unmodifiableList(result.roots()),
                        result.nextCursor()));
    }

    public Mono<ListRootsResult> listRoots(String cursor) {
        return this.session.sendRequest(McpSchema.METHOD_ROOTS_LIST, new McpSchema.PaginatedRequest(cursor), LIST_ROOTS_RESULT_TYPE_REF);
    }

    public Mono<Void> loggingNotification(LoggingMessageNotification loggingMessageNotification) {
        if (loggingMessageNotification == null) {
            return Mono.error(McpError.of("Logging message must not be null"));
        }

        return Mono.defer(() -> {
            if (this.session.isNotificationForLevelAllowed(loggingMessageNotification.level())) {
                return this.session.sendNotification(McpSchema.METHOD_NOTIFICATION_MESSAGE, loggingMessageNotification);
            }
            return Mono.empty();
        });
    }

    public Mono<Void> progressNotification(ProgressNotification progressNotification) {
        if (progressNotification == null) {
            return Mono.error(McpError.of("Progress notification must not be null"));
        }
        return this.session.sendNotification(McpSchema.METHOD_NOTIFICATION_PROGRESS, progressNotification);
    }

    public Mono<Object> ping() {
        return this.session.sendRequest(McpSchema.METHOD_PING, null, OBJECT_TYPE_REF);
    }

    void setMinLoggingLevel(LoggingLevel minLoggingLevel) {
        Assert.notNull(minLoggingLevel, "minLoggingLevel must not be null");
        this.session.setMinLoggingLevel(minLoggingLevel);
    }

    McpAsyncServerExchange copy(McpLoggableSession session, McpTransportContext transportContext) {
        return new McpAsyncServerExchange(this.sessionId(), session, this.clientCapabilities, this.clientInfo, transportContext);
    }
}