package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import top.yangxm.ai.mcp.commons.json.TypeRef;
import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpLoggableSession;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransportContext;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.exception.McpError;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.InitializeRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.InitializeResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCNotification;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCResponse;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class McpStreamableServerSession implements McpLoggableSession {
    private static final Logger logger = LoggerFactory.getLogger(McpStreamableServerSession.class);

    private final ConcurrentHashMap<Object, McpStreamableServerSessionStream> requestIdToStream = new ConcurrentHashMap<>();
    private final String id;
    private final String shortId;
    private final Duration requestTimeout;
    private final AtomicLong requestCounter = new AtomicLong(0);
    private final Map<String, McpServerRequestHandler<?>> requestHandlers;
    private final Map<String, McpServerNotificationHandler> notificationHandlers;
    private final AtomicReference<McpSchema.ClientCapabilities> clientCapabilities = new AtomicReference<>();
    private final AtomicReference<McpSchema.Implementation> clientInfo = new AtomicReference<>();
    private final AtomicReference<McpLoggableSession> listeningStreamRef;
    private final MissingMcpTransportSession missingMcpTransportSession;
    private volatile McpSchema.LoggingLevel minLoggingLevel = McpSchema.LoggingLevel.INFO;

    public McpStreamableServerSession(String id, McpSchema.ClientCapabilities clientCapabilities,
                                      McpSchema.Implementation clientInfo, Duration requestTimeout,
                                      Map<String, McpServerRequestHandler<?>> requestHandlers,
                                      Map<String, McpServerNotificationHandler> notificationHandlers) {
        this.id = id;
        this.shortId = this.id.length() > 6 ? this.id.substring(0, 6) : this.id;
        this.missingMcpTransportSession = new MissingMcpTransportSession(id);
        this.listeningStreamRef = new AtomicReference<>(this.missingMcpTransportSession);
        this.clientCapabilities.lazySet(clientCapabilities);
        this.clientInfo.lazySet(clientInfo);
        this.requestTimeout = requestTimeout;
        this.requestHandlers = requestHandlers;
        this.notificationHandlers = notificationHandlers;
    }

    public String id() {
        return id;
    }

    private String generateRequestId() {
        return this.id + "-" + this.requestCounter.getAndIncrement();
    }

    public Mono<Void> delete() {
        return this.closeGracefully().then(Mono.fromRunnable(() -> {
            // TODO: review in the context of history storage
        }));
    }

    public McpStreamableServerSessionStream listeningStream(McpStreamableServerTransport transport) {
        McpStreamableServerSessionStream listeningStream = new McpStreamableServerSessionStream(transport);
        this.listeningStreamRef.set(listeningStream);
        return listeningStream;
    }

    public Flux<McpSchema.JSONRPCMessage> replay(Object lastEventId) {
        // TODO: keep track of history by keeping a map from eventId to stream and then
        return Flux.empty();
    }

    public Mono<Void> responseStream(JSONRPCRequest request, McpStreamableServerTransport transport) {
        return Mono.deferContextual(ctx -> {
            McpTransportContext transportContext = ctx.getOrDefault(McpTransportContext.KEY, McpTransportContext.EMPTY);
            McpStreamableServerSessionStream stream = new McpStreamableServerSessionStream(transport);
            McpServerRequestHandler<?> requestHandler = McpStreamableServerSession.this.requestHandlers.get(request.method());
            // TODO: delegate to stream, which upon successful response should close
            if (requestHandler == null) {
                return transport.sendMessage(JSONRPCResponse.ofMethodNotFoundError(request.id(), request.method()));
            }
            return requestHandler
                    .handle(new McpAsyncServerExchange(this.id, stream, clientCapabilities.get(), clientInfo.get(),
                            transportContext), request.params())
                    .map(result -> JSONRPCResponse.ofSuccess(request.id(), result))
                    .onErrorResume(e -> Mono.just(JSONRPCResponse.ofInternalError(request.id(), e.getMessage())))
                    .flatMap(transport::sendMessage)
                    .then(transport.closeGracefully());
        });
    }

    public Mono<Void> accept(JSONRPCNotification notification) {
        return Mono.deferContextual(ctx -> {
            logger.debug("[{}] Received notification: {}", this.shortId, notification);
            McpTransportContext transportContext = ctx.getOrDefault(McpTransportContext.KEY, McpTransportContext.EMPTY);
            McpServerNotificationHandler notificationHandler = this.notificationHandlers.get(notification.method());
            if (notificationHandler == null) {
                logger.warn("[{}] No handler registered for notification method: {}", this.shortId, notification.method());
                return Mono.empty();
            }
            McpLoggableSession listeningStream = this.listeningStreamRef.get();
            return notificationHandler.handle(new McpAsyncServerExchange(this.id, listeningStream,
                    this.clientCapabilities.get(), this.clientInfo.get(), transportContext), notification.params());
        });
    }

    public Mono<Void> accept(JSONRPCResponse response) {
        return Mono.defer(() -> {
            logger.debug("[{}] Received response: {}", this.shortId, response);
            if (response.id() != null) {
                McpStreamableServerSessionStream stream = this.requestIdToStream.get(response.id());
                if (stream == null) {
                    String logErr = String.format("[%s] Unexpected response for unknown id %s", this.shortId, response.id());
                    logger.error(logErr);
                    return Mono.error(McpError.of(McpSchema.ErrorCodes.INTERNAL_ERROR, logErr));
                }
                // TODO: encapsulate this inside the stream itself
                MonoSink<JSONRPCResponse> sink = stream.pendingResponses.remove(response.id());
                if (sink == null) {
                    String logErr = String.format("[%s] Unexpected response for unknown id %s", this.shortId, response.id());
                    logger.error(logErr);
                    return Mono.error(McpError.of(McpSchema.ErrorCodes.INTERNAL_ERROR, logErr));
                } else {
                    sink.success(response);
                }
            } else {
                logger.error("[{}] Discarded MCP request response without session id. "
                        + "This is an indication of a bug in the request sender code that can lead to memory "
                        + "leaks as pending requests will never be completed.", this.shortId);
            }
            return Mono.empty();
        });
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
    public <T> Mono<T> sendRequest(String method, Object params, TypeRef<T> typeRef) {
        return Mono.defer(() -> {
            McpLoggableSession listeningStream = this.listeningStreamRef.get();
            return listeningStream.sendRequest(method, params, typeRef);
        });
    }

    @Override
    public Mono<Void> sendNotification(String method, Object params) {
        return Mono.defer(() -> {
            McpLoggableSession listeningStream = this.listeningStreamRef.get();
            return listeningStream.sendNotification(method, params);
        });
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.defer(() -> {
            McpLoggableSession listeningStream = this.listeningStreamRef.getAndSet(missingMcpTransportSession);
            return listeningStream.closeGracefully();
            // TODO: Also close all the open streams
        });
    }

    @Override
    public void close() {
        McpLoggableSession listeningStream = this.listeningStreamRef.getAndSet(missingMcpTransportSession);
        if (listeningStream != null) {
            listeningStream.close();
        }
        // TODO: Also close all open streams
    }

    public final class McpStreamableServerSessionStream implements McpLoggableSession {
        private final ConcurrentHashMap<Object, MonoSink<JSONRPCResponse>> pendingResponses = new ConcurrentHashMap<>();
        private final McpStreamableServerSession outerSession;
        private final McpStreamableServerTransport transport;
        private final String transportId;
        private final Supplier<String> uuidGenerator;

        public McpStreamableServerSessionStream(McpStreamableServerTransport transport) {
            this.outerSession = McpStreamableServerSession.this;
            this.transport = transport;
            this.transportId = UUID.randomUUID().toString();
            this.uuidGenerator = () -> this.transportId + "_" + UUID.randomUUID();
        }

        @Override
        public void setMinLoggingLevel(McpSchema.LoggingLevel minLoggingLevel) {
            Assert.notNull(minLoggingLevel, "minLoggingLevel must not be null");
            this.outerSession.setMinLoggingLevel(minLoggingLevel);
        }

        @Override
        public boolean isNotificationForLevelAllowed(McpSchema.LoggingLevel loggingLevel) {
            return this.outerSession.isNotificationForLevelAllowed(loggingLevel);
        }

        @Override
        public <T> Mono<T> sendRequest(String method, Object params, TypeRef<T> typeRef) {
            String requestId = this.outerSession.generateRequestId();
            this.outerSession.requestIdToStream.put(requestId, this);
            return Mono.<JSONRPCResponse>create(sink -> {
                this.pendingResponses.put(requestId, sink);
                JSONRPCRequest jsonrpcRequest = JSONRPCRequest.of(method, requestId, params);
                String messageId = this.uuidGenerator.get();
                logger.debug("[{}] Sending request, method: {}, params: {}, type: {}, requestId: {}, messageId: {}",
                        this.outerSession.shortId, method, params, typeRef.getType(), requestId, messageId);
                // TODO: store message in history
                this.transport.sendMessage(jsonrpcRequest, messageId).subscribe(v -> {
                }, sink::error);
            }).timeout(requestTimeout).doOnError(e -> {
                this.pendingResponses.remove(requestId);
                this.outerSession.requestIdToStream.remove(requestId);
            }).handle((jsonRpcResponse, sink) -> {
                if (jsonRpcResponse.error() != null) {
                    sink.error(new McpError(jsonRpcResponse.error()));
                } else {
                    if (typeRef.getType().equals(Void.class)) {
                        sink.complete();
                    } else {
                        sink.next(this.transport.unmarshalFrom(jsonRpcResponse.result(), typeRef));
                    }
                }
            });
        }

        @Override
        public Mono<Void> sendNotification(String method, Object params) {
            logger.debug("[{}] Sending notification, method: {}, params: {}", this.outerSession.shortId, method, params);
            JSONRPCNotification jsonrpcNotification = JSONRPCNotification.of(method, params);
            String messageId = this.uuidGenerator.get();
            // TODO: store message in history
            return this.transport.sendMessage(jsonrpcNotification, messageId);
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.defer(() -> {
                this.pendingResponses.values().forEach(s -> s.error(new RuntimeException("Stream closed")));
                this.pendingResponses.clear();
                this.outerSession.listeningStreamRef.compareAndSet(this, this.outerSession.missingMcpTransportSession);
                this.outerSession.requestIdToStream.values().removeIf(this::equals);
                return this.transport.closeGracefully();
            });
        }

        @Override
        public void close() {
            this.pendingResponses.values().forEach(s -> s.error(new RuntimeException("Stream closed")));
            this.pendingResponses.clear();
            this.outerSession.listeningStreamRef.compareAndSet(this, this.outerSession.missingMcpTransportSession);
            this.outerSession.requestIdToStream.values().removeIf(this::equals);
            this.transport.close();
        }
    }


    public final static class McpStreamableServerSessionInit {
        private final McpStreamableServerSession session;
        private final Mono<InitializeResult> initResult;

        public McpStreamableServerSessionInit(McpStreamableServerSession session, Mono<InitializeResult> initResult) {
            this.session = session;
            this.initResult = initResult;
        }

        public McpStreamableServerSession session() {
            return session;
        }

        public Mono<InitializeResult> initResult() {
            return initResult;
        }
    }

    public interface Factory {
        McpStreamableServerSessionInit startSession(InitializeRequest initRequest);
    }
}
