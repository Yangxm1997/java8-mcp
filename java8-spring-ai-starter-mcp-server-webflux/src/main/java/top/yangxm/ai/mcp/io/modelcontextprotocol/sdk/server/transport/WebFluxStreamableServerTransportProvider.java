package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.transport;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.json.JsonException;
import top.yangxm.ai.mcp.commons.json.JsonMapper;
import top.yangxm.ai.mcp.commons.json.TypeRef;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;
import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.commons.util.Lists;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.KeepAliveScheduler;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransportContext;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.exception.McpError;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.InitializeRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCMessage;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCNotification;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCResponse;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.ProtocolVersions;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStreamableServerSession;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStreamableServerSession.McpStreamableServerSessionInit;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStreamableServerSession.McpStreamableServerSessionStream;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStreamableServerTransport;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStreamableServerTransportProvider;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpTransportContextExtractor;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public class WebFluxStreamableServerTransportProvider implements McpStreamableServerTransportProvider {
    private static final Logger logger = LoggerFactoryHolder.getLogger(WebFluxStreamableServerTransportProvider.class);

    private final JsonMapper jsonMapper;
    private final String messageEndpoint;
    private final boolean disallowDelete;
    private final RouterFunction<?> routerFunction;
    private final ConcurrentHashMap<String, McpStreamableServerSession> sessions = new ConcurrentHashMap<>();
    private final McpTransportContextExtractor<ServerRequest> contextExtractor;
    private final KeepAliveScheduler keepAliveScheduler;
    private volatile boolean isClosing = false;
    private McpStreamableServerSession.Factory sessionFactory;

    private WebFluxStreamableServerTransportProvider(JsonMapper jsonMapper, String messageEndpoint,
                                                     boolean disallowDelete, Duration keepAliveInterval,
                                                     McpTransportContextExtractor<ServerRequest> contextExtractor) {
        Assert.notNull(jsonMapper, "JsonMapper must not be null");
        Assert.notNull(messageEndpoint, "Message endpoint must not be null");
        Assert.notNull(contextExtractor, "Context extractor must not be null");

        this.jsonMapper = jsonMapper;
        this.messageEndpoint = messageEndpoint;
        this.contextExtractor = contextExtractor;
        this.disallowDelete = disallowDelete;
        this.routerFunction = RouterFunctions.route()
                .GET(this.messageEndpoint, this::handleGet)
                .POST(this.messageEndpoint, this::handlePost)
                .DELETE(this.messageEndpoint, this::handleDelete)
                .build();

        if (keepAliveInterval != null) {
            this.keepAliveScheduler = KeepAliveScheduler
                    .builder(() -> (isClosing) ? Flux.empty() : Flux.fromIterable(sessions.values()))
                    .initialDelay(keepAliveInterval)
                    .interval(keepAliveInterval)
                    .build();
            this.keepAliveScheduler.start();
        } else {
            this.keepAliveScheduler = null;
        }
        logger.debug("WebFlux STREAMABLE transport provider initialized with messageEndpoint: {}, keepAliveInterval: {}, disallowDelete: {}",
                messageEndpoint, keepAliveInterval, disallowDelete);
    }

    public String messageEndpoint() {
        return messageEndpoint;
    }

    public boolean isDisallowDelete() {
        return disallowDelete;
    }

    private Mono<ServerResponse> handleGet(ServerRequest request) {
        if (isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).bodyValue("Server is shutting down");
        }
        McpTransportContext transportContext = this.contextExtractor.extract(request);

        return Mono.defer(() -> {
            List<MediaType> acceptHeaders = request.headers().asHttpHeaders().getAccept();
            if (!acceptHeaders.contains(MediaType.TEXT_EVENT_STREAM)) {
                return ServerResponse.badRequest().build();
            }

            if (!request.headers().asHttpHeaders().containsKey(McpTransportConst.HEADER_MCP_SESSION_ID)) {
                return ServerResponse.badRequest().build(); // TODO: say we need a session
            }

            String sessionId = request.headers().asHttpHeaders().getFirst(McpTransportConst.HEADER_MCP_SESSION_ID);
            McpStreamableServerSession session = this.sessions.get(sessionId);
            if (session == null) {
                return ServerResponse.notFound().build();
            }

            if (request.headers().asHttpHeaders().containsKey(McpTransportConst.HEADER_LAST_EVENT_ID)) {
                String lastId = request.headers().asHttpHeaders().getFirst(McpTransportConst.HEADER_LAST_EVENT_ID);
                return ServerResponse.ok()
                        .contentType(MediaType.TEXT_EVENT_STREAM)
                        .body(session.replay(lastId)
                                .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext)), ServerSentEvent.class);
            }

            return ServerResponse.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(Flux.<ServerSentEvent<?>>create(sink -> {
                        WebFluxMcpTransport sessionTransport = new WebFluxMcpTransport(sessionId, sink);
                        McpStreamableServerSessionStream listeningStream = session.listeningStream(sessionTransport);
                        sink.onDispose(listeningStream::close);
                        // TODO Clarify why the outer context is not present in the
                    }).contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext)), ServerSentEvent.class);

        }).contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext));
    }

    private Mono<ServerResponse> handlePost(ServerRequest request) {
        if (isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).bodyValue("Server is shutting down");
        }
        McpTransportContext transportContext = this.contextExtractor.extract(request);

        List<MediaType> acceptHeaders = request.headers().asHttpHeaders().getAccept();
        if (!(acceptHeaders.contains(MediaType.APPLICATION_JSON)
                && acceptHeaders.contains(MediaType.TEXT_EVENT_STREAM))) {
            return ServerResponse.badRequest().build();
        }

        return request.bodyToMono(String.class).flatMap(body -> {
                    try {
                        JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(jsonMapper, body);
                        if (message instanceof JSONRPCRequest) {
                            JSONRPCRequest jsonrpcRequest = (JSONRPCRequest) message;
                            if (jsonrpcRequest.method().equals(McpSchema.METHOD_INITIALIZE)) {
                                InitializeRequest initializeRequest = jsonMapper.convertValue(jsonrpcRequest.params(), InitializeRequest.class);
                                McpStreamableServerSessionInit init = this.sessionFactory.startSession(initializeRequest);
                                sessions.put(init.session().id(), init.session());
                                return init.initResult()
                                        .map(initializeResult -> {
                                            try {
                                                return this.jsonMapper.writeValueAsString(
                                                        JSONRPCResponse.ofSuccess(jsonrpcRequest.id(), initializeResult)
                                                );
                                            } catch (Exception e) {
                                                logger.warn("Failed to serialize initResponse", e);
                                                throw Exceptions.propagate(e);
                                            }
                                        })
                                        .flatMap(initResult -> ServerResponse.ok()
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .header(McpTransportConst.HEADER_MCP_SESSION_ID, init.session().id())
                                                .bodyValue(initResult));
                            }
                        }

                        if (!request.headers().asHttpHeaders().containsKey(McpTransportConst.HEADER_MCP_SESSION_ID)) {
                            return ServerResponse.badRequest().bodyValue(McpError.of("Session ID missing"));
                        }

                        String sessionId = request.headers().asHttpHeaders().getFirst(McpTransportConst.HEADER_MCP_SESSION_ID);
                        McpStreamableServerSession session = sessions.get(sessionId);

                        if (session == null) {
                            return ServerResponse.status(HttpStatus.NOT_FOUND)
                                    .bodyValue(McpError.of("Session not found: " + sessionId));
                        }

                        if (message instanceof JSONRPCResponse) {
                            return session.accept((JSONRPCResponse) message).then(ServerResponse.accepted().build());
                        } else if (message instanceof JSONRPCNotification) {
                            return session.accept((JSONRPCNotification) message).then(ServerResponse.accepted().build());
                        } else if (message instanceof JSONRPCRequest) {
                            JSONRPCRequest jsonrpcRequest = (JSONRPCRequest) message;
                            return ServerResponse.ok()
                                    .contentType(MediaType.TEXT_EVENT_STREAM)
                                    .body(Flux.<ServerSentEvent<?>>create(sink -> {
                                                WebFluxMcpTransport st = new WebFluxMcpTransport(sessionId, sink);
                                                Mono<Void> stream = session.responseStream(jsonrpcRequest, st);
                                                Disposable streamSubscription = stream.onErrorComplete(err -> {
                                                    sink.error(err);
                                                    return true;
                                                }).contextWrite(sink.contextView()).subscribe();
                                                sink.onCancel(streamSubscription);
                                                // TODO Clarify why the outer context is not present in the
                                            }).contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext)),
                                            ServerSentEvent.class);
                        } else {
                            return ServerResponse.badRequest().bodyValue(McpError.of("Unknown message type"));
                        }
                    } catch (JsonException | IllegalArgumentException e) {
                        logger.error("Failed to deserialize message: {}", e.getMessage());
                        return ServerResponse.badRequest().bodyValue(McpError.of("Invalid message format"));
                    }
                })
                .switchIfEmpty(ServerResponse.badRequest().build())
                .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext));
    }

    private Mono<ServerResponse> handleDelete(ServerRequest request) {
        if (isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).bodyValue("Server is shutting down");
        }
        McpTransportContext transportContext = this.contextExtractor.extract(request);

        return Mono.defer(() -> {
            if (!request.headers().asHttpHeaders().containsKey(McpTransportConst.HEADER_MCP_SESSION_ID)) {
                return ServerResponse.badRequest().build(); // TODO: say we need a session
            }

            if (this.disallowDelete) {
                return ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED).build();
            }

            String sessionId = request.headers().asHttpHeaders().getFirst(McpTransportConst.HEADER_MCP_SESSION_ID);
            McpStreamableServerSession session = this.sessions.get(sessionId);
            if (session == null) {
                return ServerResponse.notFound().build();
            }
            return session.delete().then(ServerResponse.ok().build());
        }).contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext));
    }

    @Override
    public List<String> protocolVersions() {
        return Lists.of(ProtocolVersions.MCP_2024_11_05,
                ProtocolVersions.MCP_2025_03_26,
                ProtocolVersions.MCP_2025_06_18);
    }

    @Override
    public void setSessionFactory(McpStreamableServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        if (sessions.isEmpty()) {
            logger.debug("No active sessions to broadcast message to");
            return Mono.empty();
        }
        logger.debug("Attempting to broadcast message to {} active sessions", sessions.size());
        return Flux.fromIterable(sessions.values())
                .flatMap(session -> session.sendNotification(method, params)
                        .doOnError(e -> logger.error("Failed to send message to session {}: {}", session.id(), e.getMessage()))
                        .onErrorComplete())
                .then();
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.defer(() -> {
            this.isClosing = true;
            return Flux.fromIterable(sessions.values())
                    .doFirst(() -> logger.debug("Initiating graceful shutdown with {} active sessions", sessions.size()))
                    .flatMap(McpStreamableServerSession::closeGracefully)
                    .then();
        }).then().doOnSuccess(v -> {
            sessions.clear();
            if (this.keepAliveScheduler != null) {
                this.keepAliveScheduler.shutdown();
            }
        });
    }

    public RouterFunction<?> getRouterFunction() {
        return this.routerFunction;
    }

    private class WebFluxMcpTransport implements McpStreamableServerTransport {
        private final String sessionId;
        private final FluxSink<ServerSentEvent<?>> sink;

        public WebFluxMcpTransport(String sessionId, FluxSink<ServerSentEvent<?>> sink) {
            this.sessionId = sessionId;
            this.sink = sink;
        }

        @Override
        public Mono<Void> sendMessage(JSONRPCMessage message) {
            return this.sendMessage(message, null);
        }

        @Override
        public Mono<Void> sendMessage(JSONRPCMessage message, String messageId) {
            return Mono.fromSupplier(() -> {
                try {
                    return jsonMapper.writeValueAsString(message);
                } catch (Exception e) {
                    throw Exceptions.propagate(e);
                }
            }).doOnNext(jsonText -> {
                ServerSentEvent<Object> event = ServerSentEvent.builder()
                        .id(messageId)
                        .event(McpTransportConst.MESSAGE_EVENT_TYPE)
                        .data(jsonText)
                        .build();
                sink.next(event);
            }).doOnError(e -> {
                // TODO log with sessionId
                Throwable exception = Exceptions.unwrap(e);
                sink.error(exception);
            }).then();
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
            return jsonMapper.convertValue(data, typeRef);
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(sink::complete);
        }

        @Override
        public void close() {
            sink.complete();
        }

        @Override
        public String sessionId() {
            return sessionId;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private JsonMapper jsonMapper = JsonMapper.getDefault();
        private String messageEndpoint = McpTransportConst.DEFAULT_STREAMABLE_MESSAGE_ENDPOINT;
        private McpTransportContextExtractor<ServerRequest> contextExtractor = (serverRequest) -> McpTransportContext.EMPTY;
        private boolean disallowDelete;
        private Duration keepAliveInterval;

        private Builder() {
        }

        public Builder jsonMapper(JsonMapper jsonMapper) {
            Assert.notNull(jsonMapper, "JsonMapper must not be null");
            this.jsonMapper = jsonMapper;
            return this;
        }

        public Builder messageEndpoint(String messageEndpoint) {
            Assert.notNull(messageEndpoint, "Message endpoint must not be empty");
            this.messageEndpoint = messageEndpoint;
            return this;
        }

        public Builder contextExtractor(McpTransportContextExtractor<ServerRequest> contextExtractor) {
            Assert.notNull(contextExtractor, "Context extractor must not be null");
            this.contextExtractor = contextExtractor;
            return this;
        }

        public Builder disallowDelete(boolean disallowDelete) {
            this.disallowDelete = disallowDelete;
            return this;
        }

        public Builder keepAliveInterval(Duration keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
            return this;
        }

        public WebFluxStreamableServerTransportProvider build() {
            Assert.notNull(this.messageEndpoint, "Message endpoint must be set");
            return new WebFluxStreamableServerTransportProvider(jsonMapper, messageEndpoint, disallowDelete, keepAliveInterval, contextExtractor);
        }
    }
}
