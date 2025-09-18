package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Sinks;
import top.yangxm.ai.mcp.commons.json.TypeRef;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;
import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpLoggableSession;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransportContext;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.exception.McpError;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ClientCapabilities;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Implementation;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.InitializeRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCMessage;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCNotification;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCResponse;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.LoggingLevel;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
public class McpServerSession implements McpLoggableSession {
    private static final Logger logger = LoggerFactoryHolder.getLogger(McpServerSession.class);
    private static final TypeRef<InitializeRequest> INITIALIZE_REQUEST_TYPE_REF = new TypeRef<InitializeRequest>() {
    };

    private final ConcurrentHashMap<Object, MonoSink<JSONRPCResponse>> pendingResponses = new ConcurrentHashMap<>();
    private final String id;
    private final String shortId;
    private final Duration requestTimeout;
    private final AtomicLong requestCounter = new AtomicLong(0);
    private final McpServerInitRequestHandler initRequestHandler;
    private final Map<String, McpServerRequestHandler<?>> requestHandlers;
    private final Map<String, McpServerNotificationHandler> notificationHandlers;
    private final McpServerSessionTransport sessionTransport;
    private final Sinks.One<McpAsyncServerExchange> exchangeSink = Sinks.one();
    private final AtomicReference<ClientCapabilities> clientCapabilities = new AtomicReference<>();
    private final AtomicReference<Implementation> clientInfo = new AtomicReference<>();
    private final AtomicInteger state = new AtomicInteger(STATE_UNINITIALIZED);
    private volatile LoggingLevel minLoggingLevel = LoggingLevel.INFO;

    private static final int STATE_UNINITIALIZED = 0;
    private static final int STATE_INITIALIZING = 1;
    private static final int STATE_INITIALIZED = 2;

    McpServerSession(Duration requestTimeout, McpServerSessionTransport sessionTransport,
                     McpServerInitRequestHandler initRequestHandler,
                     Map<String, McpServerRequestHandler<?>> requestHandlers,
                     Map<String, McpServerNotificationHandler> notificationHandlers) {
        this.id = sessionTransport.sessionId();
        this.shortId = this.id.length() > 6 ? this.id.substring(0, 6) : this.id;
        this.requestTimeout = requestTimeout;
        this.initRequestHandler = initRequestHandler;
        this.requestHandlers = requestHandlers;
        this.notificationHandlers = notificationHandlers;
        this.sessionTransport = sessionTransport;
    }

    public String id() {
        return id;
    }

    public void init(ClientCapabilities clientCapabilities, Implementation clientInfo) {
        this.clientCapabilities.lazySet(clientCapabilities);
        this.clientInfo.lazySet(clientInfo);
    }

    private String generateRequestId() {
        return this.id + "-" + this.requestCounter.getAndIncrement();
    }

    public Mono<Void> handle(JSONRPCMessage message) {
        return Mono.deferContextual(ctx -> {
            McpTransportContext transportContext = ctx.getOrDefault(McpTransportContext.KEY, McpTransportContext.EMPTY);
            // TODO handle errors for communication to without initialization happening
            if (message instanceof JSONRPCResponse) {
                JSONRPCResponse response = (JSONRPCResponse) message;
                logger.debug("[{}] Received Response: {}", this.shortId, response);
                MonoSink<JSONRPCResponse> sink = pendingResponses.remove(response.id());
                if (sink == null) {
                    logger.warn("[{}] Unexpected response for unknown id {}", this.shortId, response.id());
                } else {
                    sink.success(response);
                }
                return Mono.empty();
            } else if (message instanceof JSONRPCRequest) {
                JSONRPCRequest request = (JSONRPCRequest) message;
                logger.info("[{}] Received request: {}", this.shortId, request);
                return handleIncomingRequest(request, transportContext)
                        .onErrorResume(error -> {
                            logger.error("[{}] Error handling request: {}", this.shortId, error.getMessage(), error);
                            JSONRPCResponse errorResponse = JSONRPCResponse.ofInternalError(request.id(), error.getMessage());
                            // TODO: Should the error go to SSE or back as POST return?
                            return this.sessionTransport.sendMessage(errorResponse).then(Mono.empty());
                        })
                        .flatMap(this.sessionTransport::sendMessage);
            } else if (message instanceof JSONRPCNotification) {
                JSONRPCNotification notification = (JSONRPCNotification) message;
                // TODO handle errors for communication to without initialization
                logger.debug("[{}] Received notification: {}", this.shortId, notification);
                // TODO: in case of error, should the POST request be signalled?
                return handleIncomingNotification(notification, transportContext)
                        .doOnError(error -> logger.error("[{}] Error handling notification: {}", this.shortId, error.getMessage()));
            } else {
                logger.warn("[{}] Received unknown message type: {}", this.shortId, message);
                return Mono.empty();
            }
        });
    }

    private Mono<JSONRPCResponse> handleIncomingRequest(JSONRPCRequest request, McpTransportContext transportContext) {
        return Mono.defer(() -> {
            logger.debug("[{}] Handling {} request", this.shortId, request.method());
            Mono<?> resultMono;
            if (McpSchema.METHOD_INITIALIZE.equals(request.method())) {
                // TODO handle situation where already initialized!
                InitializeRequest initializeRequest = this.sessionTransport.unmarshalFrom(request.params(), INITIALIZE_REQUEST_TYPE_REF);
                this.state.lazySet(STATE_INITIALIZING);
                this.init(initializeRequest.capabilities(), initializeRequest.clientInfo());
                resultMono = this.initRequestHandler.handle(initializeRequest);
            } else {
                // TODO handle errors for communication to this session without
                McpServerRequestHandler<?> handler = this.requestHandlers.get(request.method());
                if (handler == null) {
                    logger.warn("[{}] Handle {} request failed, method not found, requestId: {}", this.shortId, request.method(), request.id());
                    return Mono.just(JSONRPCResponse.ofMethodNotFoundError(request.id(), request.method()));
                }

                resultMono = this.exchangeSink.asMono()
                        .flatMap(exchange -> handler.handle(exchange.copy(this, transportContext), request.params()));
            }
            return resultMono
                    .map(result -> {
                        JSONRPCResponse response = JSONRPCResponse.ofSuccess(request.id(), result);
                        logger.info("[{}] Handle {} request success, response: {}", this.shortId, request.method(), response);
                        return response;
                    })
                    .onErrorResume(error -> {
                        logger.error("[{}] Handle {} request failed, requestId: {} error: {}",
                                this.shortId, request.method(), request.id(), error.getMessage());
                        return Mono.just(JSONRPCResponse.ofInternalError(request.id(), error.getMessage()));
                    });
        });
    }

    private Mono<Void> handleIncomingNotification(JSONRPCNotification notification, McpTransportContext transportContext) {
        return Mono.defer(() -> {
            logger.debug("[{}] Handling {} notification", this.shortId, notification.method());
            if (McpSchema.METHOD_NOTIFICATION_INITIALIZED.equals(notification.method())) {
                this.state.lazySet(STATE_INITIALIZED);
                exchangeSink.tryEmitValue(new McpAsyncServerExchange(
                        this.id, this, clientCapabilities.get(), clientInfo.get(), transportContext)
                );
            }

            McpServerNotificationHandler handler = notificationHandlers.get(notification.method());
            if (handler == null) {
                logger.warn("[{}] Handle {} notification failed, method not found", this.shortId, notification.method());
                return Mono.empty();
            }
            return this.exchangeSink.asMono()
                    .flatMap(exchange -> handler.handle(exchange.copy(this, transportContext), notification.params()));
        });
    }

    @Override
    public void setMinLoggingLevel(LoggingLevel minLoggingLevel) {
        Assert.notNull(minLoggingLevel, "minLoggingLevel must not be null");
        this.minLoggingLevel = minLoggingLevel;
    }

    @Override
    public boolean isNotificationForLevelAllowed(LoggingLevel loggingLevel) {
        return loggingLevel.level() >= this.minLoggingLevel.level();
    }

    @Override
    public <T> Mono<T> sendRequest(String method, Object params, TypeRef<T> typeRef) {
        String requestId = this.generateRequestId();
        logger.debug("[{}] Sending request, method: {}, params: {}, type: {}, requestId: {}", this.shortId, method, params, typeRef.getType(), requestId);
        return Mono.<JSONRPCResponse>create(sink -> {
            this.pendingResponses.put(requestId, sink);
            JSONRPCRequest request = JSONRPCRequest.of(method, requestId, params);
            this.sessionTransport.sendMessage(request).subscribe(v -> {
            }, error -> {
                this.pendingResponses.remove(requestId);
                sink.error(error);
            });
        }).timeout(requestTimeout).handle((response, sink) -> {
            if (response.error() != null) {
                sink.error(new McpError(response.error()));
            } else {
                if (typeRef.getType().equals(Void.class)) {
                    sink.complete();
                } else {
                    sink.next(this.sessionTransport.unmarshalFrom(response.result(), typeRef));
                }
            }
        });
    }

    @Override
    public Mono<Void> sendNotification(String method, Object params) {
        logger.debug("[{}] Sending notification, method: {}, params: {}", this.id, method, params);
        return this.sessionTransport.sendMessage(JSONRPCNotification.of(method, params));
    }

    @Override
    public Mono<Void> closeGracefully() {
        // TODO: clear pendingResponses and emit errors?
        return this.sessionTransport.closeGracefully();
    }

    @Override
    public void close() {
        // TODO: clear pendingResponses and emit errors?
        this.sessionTransport.close();
    }
}
