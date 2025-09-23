package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.transport;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import reactor.core.publisher.Flux;
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
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStreamableServerSession.McpStreamableServerSessionStream;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStreamableServerTransport;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStreamableServerTransportProvider;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpTransportContextExtractor;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class WebMvcStreamableServerTransportProvider implements McpStreamableServerTransportProvider {
    private static final Logger logger = LoggerFactoryHolder.getLogger(WebMvcStreamableServerTransportProvider.class);
    private final JsonMapper jsonMapper;
    private final String messageEndpoint;
    private final boolean disallowDelete;
    private final RouterFunction<ServerResponse> routerFunction;
    private final ConcurrentHashMap<String, McpStreamableServerSession> sessions = new ConcurrentHashMap<>();
    private final McpTransportContextExtractor<ServerRequest> contextExtractor;
    private final KeepAliveScheduler keepAliveScheduler;
    private volatile boolean isClosing = false;
    private McpStreamableServerSession.Factory sessionFactory;

    private WebMvcStreamableServerTransportProvider(JsonMapper jsonMapper, String messageEndpoint,
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
        logger.debug("WebMVC STREAMABLE transport provider initialized with messageEndpoint: {}, keepAliveInterval: {}, disallowDelete: {}",
                messageEndpoint, keepAliveInterval, disallowDelete);
    }

    public String messageEndpoint() {
        return messageEndpoint;
    }

    public boolean isDisallowDelete() {
        return disallowDelete;
    }

    private ServerResponse handleGet(ServerRequest request) {
        if (this.isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
        }

        List<MediaType> acceptHeaders = request.headers().asHttpHeaders().getAccept();
        if (!acceptHeaders.contains(MediaType.TEXT_EVENT_STREAM)) {
            return ServerResponse.badRequest().body("Invalid Accept header. Expected TEXT_EVENT_STREAM");
        }
        McpTransportContext transportContext = this.contextExtractor.extract(request);

        if (!request.headers().asHttpHeaders().containsKey(McpTransportConst.HEADER_MCP_SESSION_ID)) {
            return ServerResponse.badRequest().body("Session ID required in mcp-session-id header");
        }

        String sessionId = request.headers().asHttpHeaders().getFirst(McpTransportConst.HEADER_MCP_SESSION_ID);
        McpStreamableServerSession session = this.sessions.get(sessionId);

        if (session == null) {
            return ServerResponse.notFound().build();
        }

        logger.debug("Handling GET request for session: {}", sessionId);
        try {
            return ServerResponse.sse(sseBuilder -> {
                sseBuilder.onTimeout(() -> logger.debug("SSE connection timed out for session: {}", sessionId));
                WebMvcMcpTransport sessionTransport = new WebMvcMcpTransport(sessionId, sseBuilder);
                if (request.headers().asHttpHeaders().containsKey(McpTransportConst.HEADER_LAST_EVENT_ID)) {
                    String lastId = request.headers().asHttpHeaders().getFirst(McpTransportConst.HEADER_LAST_EVENT_ID);
                    try {
                        session.replay(lastId)
                                .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                                .toIterable()
                                .forEach(message -> {
                                    try {
                                        sessionTransport.sendMessage(message)
                                                .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                                                .block();
                                    } catch (Exception e) {
                                        logger.error("Failed to replay message: {}", e.getMessage());
                                        sseBuilder.error(e);
                                    }
                                });
                    } catch (Exception e) {
                        logger.error("Failed to replay messages: {}", e.getMessage());
                        sseBuilder.error(e);
                    }
                } else {
                    McpStreamableServerSessionStream listeningStream = session.listeningStream(sessionTransport);
                    sseBuilder.onComplete(() -> {
                        logger.debug("SSE connection completed for session: {}", sessionId);
                        listeningStream.close();
                    });
                }
            }, Duration.ZERO);
        } catch (Exception e) {
            logger.error("Failed to handle GET request for session {}: {}", sessionId, e.getMessage());
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ServerResponse handlePost(ServerRequest request) {
        if (this.isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
        }

        List<MediaType> acceptHeaders = request.headers().asHttpHeaders().getAccept();
        if (!acceptHeaders.contains(MediaType.TEXT_EVENT_STREAM)
                || !acceptHeaders.contains(MediaType.APPLICATION_JSON)) {
            return ServerResponse.badRequest()
                    .body(McpError.of("Invalid Accept headers. Expected TEXT_EVENT_STREAM and APPLICATION_JSON"));
        }

        McpTransportContext transportContext = this.contextExtractor.extract(request);

        try {
            String body = request.body(String.class);
            JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(jsonMapper, body);
            if (message instanceof JSONRPCRequest) {
                JSONRPCRequest jsonrpcRequest = (JSONRPCRequest) message;
                if (jsonrpcRequest.method().equals(McpSchema.METHOD_INITIALIZE)) {
                    InitializeRequest initializeRequest = jsonMapper.convertValue(jsonrpcRequest.params(), InitializeRequest.class);
                    McpStreamableServerSession.McpStreamableServerSessionInit init = this.sessionFactory
                            .startSession(initializeRequest);
                    this.sessions.put(init.session().id(), init.session());

                    try {
                        McpSchema.InitializeResult initResult = init.initResult().block();

                        return ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(McpTransportConst.HEADER_MCP_SESSION_ID, init.session().id())
                                .body(new JSONRPCResponse(McpSchema.JSONRPC_VERSION, jsonrpcRequest.id(), initResult,
                                        null));
                    } catch (Exception e) {
                        logger.error("Failed to initialize session: {}", e.getMessage());
                        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(McpError.of(e.getMessage()));
                    }
                }
            }

            if (!request.headers().asHttpHeaders().containsKey(McpTransportConst.HEADER_MCP_SESSION_ID)) {
                return ServerResponse.badRequest().body(McpError.of("Session ID missing"));
            }

            String sessionId = request.headers().asHttpHeaders().getFirst(McpTransportConst.HEADER_MCP_SESSION_ID);
            McpStreamableServerSession session = this.sessions.get(sessionId);

            if (session == null) {
                return ServerResponse.status(HttpStatus.NOT_FOUND)
                        .body(McpError.of("Session not found: " + sessionId));
            }

            if (message instanceof JSONRPCResponse) {
                session.accept((JSONRPCResponse) message)
                        .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                        .block();
                return ServerResponse.accepted().build();
            } else if (message instanceof JSONRPCNotification) {
                session.accept((JSONRPCNotification) message)
                        .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                        .block();
                return ServerResponse.accepted().build();
            } else if (message instanceof JSONRPCRequest) {
                JSONRPCRequest jsonrpcRequest = (JSONRPCRequest) message;
                return ServerResponse.sse(sseBuilder -> {
                    sseBuilder.onComplete(() -> logger.debug("Request response stream completed for session: {}", sessionId));
                    sseBuilder.onTimeout(() -> logger.debug("Request response stream timed out for session: {}", sessionId));
                    WebMvcMcpTransport sessionTransport = new WebMvcMcpTransport(sessionId, sseBuilder);
                    try {
                        session.responseStream(jsonrpcRequest, sessionTransport)
                                .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                                .block();
                    } catch (Exception e) {
                        logger.error("Failed to handle request stream: {}", e.getMessage());
                        sseBuilder.error(e);
                    }
                }, Duration.ZERO);
            } else {
                return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(McpError.of("Unknown message type"));
            }
        } catch (JsonException | IllegalArgumentException e) {
            logger.error("Failed to deserialize message: {}", e.getMessage());
            return ServerResponse.badRequest().body(McpError.of("Invalid message format"));
        } catch (Exception e) {
            logger.error("Error handling message: {}", e.getMessage());
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(McpError.of(e.getMessage()));
        }
    }

    private ServerResponse handleDelete(ServerRequest request) {
        if (this.isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
        }

        if (this.disallowDelete) {
            return ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        McpTransportContext transportContext = this.contextExtractor.extract(request);

        if (!request.headers().asHttpHeaders().containsKey(McpTransportConst.HEADER_MCP_SESSION_ID)) {
            return ServerResponse.badRequest().body("Session ID required in mcp-session-id header");
        }

        String sessionId = request.headers().asHttpHeaders().getFirst(McpTransportConst.HEADER_MCP_SESSION_ID);
        if (sessionId == null) {
            return ServerResponse.notFound().build();
        }

        McpStreamableServerSession session = this.sessions.get(sessionId);
        if (session == null) {
            return ServerResponse.notFound().build();
        }

        try {
            session.delete().contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext)).block();
            this.sessions.remove(sessionId);
            return ServerResponse.ok().build();
        } catch (Exception e) {
            logger.error("Failed to delete session {}: {}", sessionId, e.getMessage());
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(McpError.of(e.getMessage()));
        }
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
        if (this.sessions.isEmpty()) {
            logger.debug("No active sessions to broadcast message to");
            return Mono.empty();
        }

        logger.debug("Attempting to broadcast message to {} active sessions", this.sessions.size());
        return Mono.fromRunnable(() -> this.sessions.values().parallelStream().forEach(session -> {
            try {
                session.sendNotification(method, params).block();
            } catch (Exception e) {
                logger.error("Failed to send message to session {}: {}", session.id(), e.getMessage());
            }
        }));
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> {
            this.isClosing = true;
            logger.debug("Initiating graceful shutdown with {} active sessions", this.sessions.size());

            this.sessions.values().parallelStream().forEach(session -> {
                try {
                    session.closeGracefully().block();
                } catch (Exception e) {
                    logger.error("Failed to close session {}: {}", session.id(), e.getMessage());
                }
            });

            this.sessions.clear();
            logger.debug("Graceful shutdown completed");
        }).then().doOnSuccess(v -> {
            if (this.keepAliveScheduler != null) {
                this.keepAliveScheduler.shutdown();
            }
        });
    }

    public RouterFunction<ServerResponse> getRouterFunction() {
        return this.routerFunction;
    }

    private class WebMvcMcpTransport implements McpStreamableServerTransport {
        private final String sessionId;
        private final ServerResponse.SseBuilder sseBuilder;
        private final ReentrantLock lock = new ReentrantLock();
        private volatile boolean closed = false;

        WebMvcMcpTransport(String sessionId, ServerResponse.SseBuilder sseBuilder) {
            this.sessionId = sessionId;
            this.sseBuilder = sseBuilder;
            logger.debug("Streamable session transport {} initialized with SSE builder", sessionId);
        }

        @Override
        public String sessionId() {
            return sessionId;
        }

        @Override
        public Mono<Void> sendMessage(JSONRPCMessage message) {
            return sendMessage(message, null);
        }

        @Override
        public Mono<Void> sendMessage(JSONRPCMessage message, String messageId) {
            return Mono.fromRunnable(() -> {
                if (this.closed) {
                    logger.debug("Attempted to send message to closed session: {}", this.sessionId);
                    return;
                }

                this.lock.lock();
                try {
                    if (this.closed) {
                        logger.debug("Session {} was closed during message send attempt", this.sessionId);
                        return;
                    }

                    String jsonText = jsonMapper.writeValueAsString(message);
                    this.sseBuilder.id(messageId != null ? messageId : this.sessionId)
                            .event(McpTransportConst.MESSAGE_EVENT_TYPE)
                            .data(jsonText);
                    logger.debug("Message sent to session {} with ID {}", this.sessionId, messageId);
                } catch (Exception e) {
                    logger.error("Failed to send message to session {}: {}", this.sessionId, e.getMessage());
                    try {
                        this.sseBuilder.error(e);
                    } catch (Exception errorException) {
                        logger.error("Failed to send error to SSE builder for session {}: {}", this.sessionId,
                                errorException.getMessage());
                    }
                } finally {
                    this.lock.unlock();
                }
            });
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
            return jsonMapper.convertValue(data, typeRef);
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(WebMvcMcpTransport.this::close);
        }

        @Override
        public void close() {
            this.lock.lock();
            try {
                if (this.closed) {
                    logger.debug("Session transport {} already closed", this.sessionId);
                    return;
                }
                this.closed = true;
                this.sseBuilder.complete();
                logger.debug("Successfully completed SSE builder for session {}", sessionId);
            } catch (Exception e) {
                logger.warn("Failed to complete SSE builder for session {}: {}", sessionId, e.getMessage());
            } finally {
                this.lock.unlock();
            }
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

        public WebMvcStreamableServerTransportProvider build() {
            Assert.notNull(this.messageEndpoint, "Message endpoint must be set");
            return new WebMvcStreamableServerTransportProvider(jsonMapper, messageEndpoint, disallowDelete, keepAliveInterval, contextExtractor);
        }
    }
}
