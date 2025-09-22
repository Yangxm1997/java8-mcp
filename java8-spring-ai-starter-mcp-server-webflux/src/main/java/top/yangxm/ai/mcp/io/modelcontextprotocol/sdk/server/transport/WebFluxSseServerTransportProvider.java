package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.transport;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
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

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public class WebFluxSseServerTransportProvider implements McpServerTransportProvider {
    private static final Logger logger = LoggerFactoryHolder.getLogger(WebFluxSseServerTransportProvider.class);
    public static final String DEFAULT_BASE_URL = "";
    public static final String DEFAULT_SSE_ENDPOINT = "/sse";
    public static final String MESSAGE_EVENT_TYPE = "message";
    public static final String ENDPOINT_EVENT_TYPE = "endpoint";

    private final JsonMapper jsonMapper;
    private final String baseUrl;
    private final String messageEndpoint;
    private final String sseEndpoint;
    private final RouterFunction<?> routerFunction;
    private final ConcurrentHashMap<String, McpServerSession> sessions = new ConcurrentHashMap<>();
    private final McpTransportContextExtractor<ServerRequest> contextExtractor;
    private final KeepAliveScheduler keepAliveScheduler;
    private volatile boolean isClosing = false;
    private McpServerSession.Factory sessionFactory;

    private WebFluxSseServerTransportProvider(JsonMapper jsonMapper, String baseUrl, String messageEndpoint,
                                              String sseEndpoint, Duration keepAliveInterval,
                                              McpTransportContextExtractor<ServerRequest> contextExtractor) {
        Assert.notNull(jsonMapper, "JsonMapper must not be null");
        Assert.notNull(baseUrl, "baseUrl must not be null");
        Assert.notNull(messageEndpoint, "messageEndpoint must not be null");
        Assert.notNull(sseEndpoint, "sseEndpoint must not be null");
        Assert.notNull(contextExtractor, "contextExtractor must not be null");

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
        logger.debug("WebFlux SSE transport provider initialized with baseUrl: {}, messageEndpoint: {}, sseEndpoint: {}, keepAliveInterval: {}",
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

    private Mono<ServerResponse> handleSseConnection(ServerRequest request) {
        if (isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).bodyValue("Server is shutting down");
        }

        McpTransportContext transportContext = this.contextExtractor.extract(request);
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(Flux.<ServerSentEvent<?>>create(sink -> {
                    String sessionId = UUID.randomUUID().toString();
                    WebFluxMcpTransport sessionTransport = new WebFluxMcpTransport(sessionId, sink);
                    McpServerSession session = sessionFactory.create(sessionTransport);
                    logger.debug("Created new SSE connection for session: {}", sessionId);
                    sessions.put(sessionId, session);

                    logger.debug("Sending initial endpoint event to session: {}", sessionId);
                    sink.next(ServerSentEvent.builder()
                            .event(ENDPOINT_EVENT_TYPE)
                            .data(this.baseUrl + this.messageEndpoint + "?sessionId=" + sessionId)
                            .build());
                    sink.onCancel(() -> {
                        logger.debug("Session {} cancelled", sessionId);
                        sessions.remove(sessionId);
                    });
                }).contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext)), ServerSentEvent.class);
    }

    private Mono<ServerResponse> handleMessage(ServerRequest request) {
        if (isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).bodyValue("Server is shutting down");
        }

        if (!request.queryParam("sessionId").isPresent()) {
            return ServerResponse.badRequest().bodyValue(McpError.of("Session ID missing in message endpoint"));
        }

        McpServerSession session = sessions.get(request.queryParam("sessionId").get());

        if (session == null) {
            return ServerResponse.status(HttpStatus.NOT_FOUND)
                    .bodyValue(McpError.of("Session not found: " + request.queryParam("sessionId").get()));
        }

        McpTransportContext transportContext = this.contextExtractor.extract(request);

        return request.bodyToMono(String.class).flatMap(body -> {
            try {
                JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(jsonMapper, body);
                return session.handle(message).flatMap(response -> ServerResponse.ok().build()).onErrorResume(error -> {
                    logger.error("Error processing  message: {}", error.getMessage());
                    // TODO: instead of signalling the error, just respond with 200 OK
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .bodyValue(McpError.of(error.getMessage()));
                });
            } catch (Exception e) {
                logger.error("Failed to deserialize message: {}", e.getMessage());
                return ServerResponse.badRequest().bodyValue(McpError.of("Invalid message format"));
            }
        }).contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext));
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
                        .doOnError(e -> logger.error("Failed to send message to session {}: {}", session.id(), e.getMessage()))
                        .onErrorComplete())
                .then();
    }

    @Override
    public Mono<Void> closeGracefully() {
        isClosing = true;
        return Flux.fromIterable(sessions.values())
                .doFirst(() -> logger.debug("Initiating graceful shutdown with {} active sessions", sessions.size()))
                .flatMap(McpServerSession::closeGracefully)
                .then()
                .doOnSuccess(v -> {
                    logger.debug("Graceful shutdown completed");
                    sessions.clear();
                    if (this.keepAliveScheduler != null) {
                        this.keepAliveScheduler.shutdown();
                    }
                });
    }

    public RouterFunction<?> getRouterFunction() {
        return this.routerFunction;
    }

    private class WebFluxMcpTransport implements McpServerTransport {
        private final String sessionId;
        private final FluxSink<ServerSentEvent<?>> sink;

        public WebFluxMcpTransport(String sessionId, FluxSink<ServerSentEvent<?>> sink) {
            this.sessionId = sessionId;
            this.sink = sink;
        }

        @Override
        public Mono<Void> sendMessage(JSONRPCMessage message) {
            return Mono.fromSupplier(() -> {
                try {
                    return jsonMapper.writeValueAsString(message);
                } catch (Exception e) {
                    throw Exceptions.propagate(e);
                }
            }).doOnNext(jsonText -> {
                ServerSentEvent<Object> event = ServerSentEvent.builder()
                        .event(MESSAGE_EVENT_TYPE)
                        .data(jsonText)
                        .build();
                sink.next(event);
            }).doOnError(e -> {
                logger.error("Error sending message to session {}: {}", sessionId, e.getMessage(), e);
                Throwable exception = Exceptions.unwrap(e);
                sink.error(exception);
            }).then();
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(() -> {
                logger.debug("Closing session transport: {}", sessionId);
                try {
                    sessions.remove(sessionId);
                    sink.complete();
                    logger.debug("Successfully completed async context for session {}", sessionId);
                } catch (Exception e) {
                    logger.warn("Failed to complete async context for session {}: {}", sessionId, e.getMessage());
                }
            });
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
            return jsonMapper.convertValue(data, typeRef);
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
        private String baseUrl = DEFAULT_BASE_URL;
        private String messageEndpoint;
        private String sseEndpoint = DEFAULT_SSE_ENDPOINT;
        private McpTransportContextExtractor<ServerRequest> contextExtractor = (serverRequest) -> McpTransportContext.EMPTY;
        private Duration keepAliveInterval;

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

        public Builder contextExtractor(McpTransportContextExtractor<ServerRequest> contextExtractor) {
            Assert.notNull(contextExtractor, "Context extractor must not be null");
            this.contextExtractor = contextExtractor;
            return this;
        }

        public Builder keepAliveInterval(Duration keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
            return this;
        }

        public WebFluxSseServerTransportProvider build() {
            if (messageEndpoint == null) {
                throw new IllegalStateException("MessageEndpoint must be set");
            }
            return new WebFluxSseServerTransportProvider(jsonMapper == null ? JsonMapper.getDefault() : jsonMapper,
                    baseUrl, messageEndpoint, sseEndpoint, keepAliveInterval, contextExtractor);
        }
    }
}
