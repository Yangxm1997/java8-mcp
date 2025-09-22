package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.transport;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.json.JsonMapper;
import top.yangxm.ai.mcp.commons.json.TypeRef;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;
import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.KeepAliveScheduler;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransportContext;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.exception.McpError;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCMessage;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerSession;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerTransport;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerTransportProvider;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpTransportContextExtractor;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("unused")
public class WebMvcSseServerTransportProvider implements McpServerTransportProvider {
    private static final Logger logger = LoggerFactoryHolder.getLogger(WebMvcSseServerTransportProvider.class);
    public static final String DEFAULT_SSE_ENDPOINT = "/sse";
    public static final String MESSAGE_EVENT_TYPE = "message";
    public static final String ENDPOINT_EVENT_TYPE = "endpoint";


    private final JsonMapper jsonMapper;
    private final String baseUrl;
    private final String messageEndpoint;
    private final String sseEndpoint;
    private final RouterFunction<ServerResponse> routerFunction;
    private final ConcurrentHashMap<String, McpServerSession> sessions = new ConcurrentHashMap<>();
    private final McpTransportContextExtractor<ServerRequest> contextExtractor;
    private final KeepAliveScheduler keepAliveScheduler;
    private volatile boolean isClosing = false;
    private McpServerSession.Factory sessionFactory;

    private WebMvcSseServerTransportProvider(JsonMapper jsonMapper, String baseUrl, String messageEndpoint,
                                             String sseEndpoint, Duration keepAliveInterval,
                                             McpTransportContextExtractor<ServerRequest> contextExtractor) {
        Assert.notNull(jsonMapper, "McpJsonMapper must not be null");
        Assert.notNull(baseUrl, "Message base URL must not be null");
        Assert.notNull(messageEndpoint, "Message endpoint must not be null");
        Assert.notNull(sseEndpoint, "SSE endpoint must not be null");
        Assert.notNull(contextExtractor, "Context extractor must not be null");

        this.jsonMapper = jsonMapper;
        this.baseUrl = baseUrl;
        this.messageEndpoint = messageEndpoint;
        this.sseEndpoint = sseEndpoint;
        this.contextExtractor = contextExtractor;
        this.routerFunction = RouterFunctions.route()
                .GET(this.sseEndpoint, this::handleSseConnection)
                .POST(this.messageEndpoint, this::handleMessage)
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
        logger.debug("WebMVC SSE transport provider initialized with baseUrl: {}, messageEndpoint: {}, sseEndpoint: {}, keepAliveInterval: {}",
                baseUrl, messageEndpoint, sseEndpoint, keepAliveInterval);
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String messageEndpoint() {
        return messageEndpoint;
    }

    public String sseEndpoint() {
        return sseEndpoint;
    }

    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
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
                        .doOnError(e -> logger.error("Failed to send notification to session {}: {}", session.id(), e.getMessage()))
                        .onErrorComplete())
                .then();
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Flux.fromIterable(sessions.values()).doFirst(() -> {
            this.isClosing = true;
            logger.debug("Initiating graceful shutdown with {} active sessions", sessions.size());
        }).flatMap(McpServerSession::closeGracefully).then().doOnSuccess(v -> {
            logger.debug("Graceful shutdown completed");
            sessions.clear();
            if (this.keepAliveScheduler != null) {
                this.keepAliveScheduler.shutdown();
            }
        });
    }

    public RouterFunction<ServerResponse> getRouterFunction() {
        return this.routerFunction;
    }

    private ServerResponse handleSseConnection(ServerRequest request) {
        if (this.isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
        }

        String sessionId = UUID.randomUUID().toString();
        logger.debug("Creating new SSE connection for session: {}", sessionId);

        try {
            return ServerResponse.sse(sseBuilder -> {
                sseBuilder.onComplete(() -> {
                    logger.debug("SSE connection completed for session: {}", sessionId);
                    sessions.remove(sessionId);
                });
                sseBuilder.onTimeout(() -> {
                    logger.debug("SSE connection timed out for session: {}", sessionId);
                    sessions.remove(sessionId);
                });

                WebMvcMcpTransport sessionTransport = new WebMvcMcpTransport(sessionId, sseBuilder);
                McpServerSession session = sessionFactory.create(sessionTransport);
                this.sessions.put(sessionId, session);

                try {
                    sseBuilder.id(sessionId)
                            .event(ENDPOINT_EVENT_TYPE)
                            .data(this.baseUrl + this.messageEndpoint + "?sessionId=" + sessionId);
                } catch (Exception e) {
                    logger.error("Failed to send initial endpoint event: {}", e.getMessage());
                    sseBuilder.error(e);
                }
            }, Duration.ZERO);
        } catch (Exception e) {
            logger.error("Failed to send initial endpoint event to session {}: {}", sessionId, e.getMessage());
            sessions.remove(sessionId);
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ServerResponse handleMessage(ServerRequest request) {
        if (this.isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
        }

        if (!request.param("sessionId").isPresent()) {
            return ServerResponse.badRequest().body(McpError.of("Session ID missing in message endpoint"));
        }

        String sessionId = request.param("sessionId").get();
        McpServerSession session = sessions.get(sessionId);

        if (session == null) {
            return ServerResponse.status(HttpStatus.NOT_FOUND).body(McpError.of("Session not found: " + sessionId));
        }

        try {
            final McpTransportContext transportContext = this.contextExtractor.extract(request);
            String body = request.body(String.class);
            JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(jsonMapper, body);
            session.handle(message).contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext)).block();
            return ServerResponse.ok().build();
        } catch (IllegalArgumentException | IOException e) {
            logger.error("Failed to deserialize message: {}", e.getMessage());
            return ServerResponse.badRequest().body(McpError.of("Invalid message format"));
        } catch (Exception e) {
            logger.error("Error handling message: {}", e.getMessage());
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(McpError.of(e.getMessage()));
        }
    }

    private class WebMvcMcpTransport implements McpServerTransport {
        private final String sessionId;
        private final ServerResponse.SseBuilder sseBuilder;
        private final ReentrantLock sseBuilderLock = new ReentrantLock();

        WebMvcMcpTransport(String sessionId, ServerResponse.SseBuilder sseBuilder) {
            this.sessionId = sessionId;
            this.sseBuilder = sseBuilder;
            logger.debug("Session transport {} initialized with SSE builder", sessionId);
        }

        @Override
        public Mono<Void> sendMessage(JSONRPCMessage message) {
            return Mono.fromRunnable(() -> {
                sseBuilderLock.lock();
                try {
                    String jsonText = jsonMapper.writeValueAsString(message);
                    sseBuilder.id(sessionId).event(MESSAGE_EVENT_TYPE).data(jsonText);
                    logger.debug("Message sent to session {}", sessionId);
                } catch (Exception e) {
                    logger.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
                    sseBuilder.error(e);
                } finally {
                    sseBuilderLock.unlock();
                }
            });
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
            return jsonMapper.convertValue(data, typeRef);
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(this::close);
        }

        @Override
        public void close() {
            logger.debug("Closing session transport: {}", sessionId);
            sseBuilderLock.lock();
            try {
                sseBuilder.complete();
                logger.debug("Successfully completed SSE builder for session {}", sessionId);
            } catch (Exception e) {
                logger.warn("Failed to complete SSE builder for session {}: {}", sessionId, e.getMessage());
            } finally {
                sseBuilderLock.unlock();
            }
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
        private JsonMapper jsonMapper;
        private String baseUrl = "";
        private String messageEndpoint;
        private String sseEndpoint = DEFAULT_SSE_ENDPOINT;
        private Duration keepAliveInterval;
        private McpTransportContextExtractor<ServerRequest> contextExtractor = (serverRequest) -> McpTransportContext.EMPTY;

        public Builder jsonMapper(JsonMapper jsonMapper) {
            Assert.notNull(jsonMapper, "JsonMapper must not be null");
            this.jsonMapper = jsonMapper;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            Assert.notNull(baseUrl, "Base URL must not be null");
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder messageEndpoint(String messageEndpoint) {
            Assert.hasText(messageEndpoint, "Message endpoint must not be empty");
            this.messageEndpoint = messageEndpoint;
            return this;
        }

        public Builder sseEndpoint(String sseEndpoint) {
            Assert.hasText(sseEndpoint, "SSE endpoint must not be empty");
            this.sseEndpoint = sseEndpoint;
            return this;
        }

        public Builder keepAliveInterval(Duration keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
            return this;
        }

        public Builder contextExtractor(McpTransportContextExtractor<ServerRequest> contextExtractor) {
            Assert.notNull(contextExtractor, "contextExtractor must not be null");
            this.contextExtractor = contextExtractor;
            return this;
        }

        public WebMvcSseServerTransportProvider build() {
            if (messageEndpoint == null) {
                throw new IllegalStateException("MessageEndpoint must be set");
            }
            return new WebMvcSseServerTransportProvider(jsonMapper == null ? JsonMapper.getDefault() : jsonMapper,
                    baseUrl, messageEndpoint, sseEndpoint, keepAliveInterval, contextExtractor);
        }
    }
}
