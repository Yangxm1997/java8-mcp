package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.transport;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.json.McpJsonMapper;
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
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerSessionFactory;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerSessionTransport;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerSessionTransportProvider;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpTransportContextExtractor;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@WebServlet(asyncSupported = true)
public class HttpServletSseServerTransportProvider extends HttpServlet implements McpServerSessionTransportProvider {
    private static final Logger LOGGER = LoggerFactoryHolder.getLogger(HttpServletSseServerTransportProvider.class);
    public static final String UTF_8 = "UTF-8";
    public static final String APPLICATION_JSON = "application/json";
    public static final String DEFAULT_BASE_URL = "";
    public static final String DEFAULT_SSE_ENDPOINT = "/sse";
    public static final String MESSAGE_EVENT_TYPE = "message";
    public static final String ENDPOINT_EVENT_TYPE = "endpoint";

    private final McpJsonMapper jsonMapper;
    private final String baseUrl;
    private final String messageEndpoint;
    private final String sseEndpoint;
    private final Map<String, McpServerSession> sessions = new ConcurrentHashMap<>();
    private final McpTransportContextExtractor<HttpServletRequest> contextExtractor;
    private final AtomicBoolean isClosing = new AtomicBoolean(false);
    private final KeepAliveScheduler keepAliveScheduler;
    private McpServerSessionFactory sessionFactory;

    private HttpServletSseServerTransportProvider(McpJsonMapper jsonMapper, String baseUrl, String messageEndpoint,
                                                  String sseEndpoint, Duration keepAliveInterval,
                                                  McpTransportContextExtractor<HttpServletRequest> contextExtractor) {
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
        if (keepAliveInterval != null) {
            this.keepAliveScheduler = KeepAliveScheduler
                    .builder(() -> (isClosing.get()) ? Flux.empty() : Flux.fromIterable(sessions.values()))
                    .initialDelay(keepAliveInterval)
                    .interval(keepAliveInterval)
                    .build();

            this.keepAliveScheduler.start();
        } else {
            this.keepAliveScheduler = null;
        }

        LOGGER.debug("Http servlet SSE transport provider initialized with baseUrl: {}, messageEndpoint: {}, sseEndpoint: {}, keepAliveInterval: {}",
                baseUrl, messageEndpoint, sseEndpoint, keepAliveInterval);
    }

    @Override
    public void setSessionFactory(McpServerSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        if (sessions.isEmpty()) {
            LOGGER.debug("No active sessions to broadcast message to");
            return Mono.empty();
        }

        LOGGER.debug("Attempting to broadcast message to {} active sessions", sessions.size());
        return Flux.fromIterable(sessions.values())
                .flatMap(session -> session.sendNotification(method, params)
                        .doOnError(e -> LOGGER.error("Failed to send notification to session {}: {}", session.id(), e.getMessage()))
                        .onErrorComplete())
                .then();
    }

    @Override
    public Mono<Void> closeGracefully() {
        isClosing.set(true);
        LOGGER.debug("Initiating graceful shutdown with {} active sessions", sessions.size());
        return Flux.fromIterable(sessions.values()).flatMap(McpServerSession::closeGracefully).then().doOnSuccess(v -> {
            sessions.clear();
            LOGGER.debug("Graceful shutdown completed");
            if (this.keepAliveScheduler != null) {
                this.keepAliveScheduler.shutdown();
            }
        });
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        if (!requestURI.endsWith(sseEndpoint)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (isClosing.get()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Server is shutting down");
            return;
        }

        response.setContentType("text/event-stream");
        response.setCharacterEncoding(UTF_8);
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Access-Control-Allow-Origin", "*");

        String sessionId = UUID.randomUUID().toString();
        AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout(0);

        PrintWriter writer = response.getWriter();
        HttpServletMcpSessionTransport sessionTransport = new HttpServletMcpSessionTransport(sessionId, asyncContext, writer);
        McpServerSession session = sessionFactory.create(sessionTransport);
        this.sessions.put(sessionId, session);
        this.sendEvent(writer, ENDPOINT_EVENT_TYPE, this.baseUrl + this.messageEndpoint + "?sessionId=" + sessionId);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (isClosing.get()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Server is shutting down");
            return;
        }

        String requestURI = request.getRequestURI();
        if (!requestURI.endsWith(messageEndpoint)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String sessionId = request.getParameter("sessionId");
        if (sessionId == null) {
            response.setContentType(APPLICATION_JSON);
            response.setCharacterEncoding(UTF_8);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            String jsonError = jsonMapper.writeValueAsString(McpError.of("Session ID missing in message endpoint"));
            PrintWriter writer = response.getWriter();
            writer.write(jsonError);
            writer.flush();
            return;
        }

        McpServerSession session = sessions.get(sessionId);
        if (session == null) {
            response.setContentType(APPLICATION_JSON);
            response.setCharacterEncoding(UTF_8);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            String jsonError = jsonMapper.writeValueAsString(McpError.of("Session not found: " + sessionId));
            PrintWriter writer = response.getWriter();
            writer.write(jsonError);
            writer.flush();
            return;
        }

        try {
            BufferedReader reader = request.getReader();
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }

            final McpTransportContext transportContext = this.contextExtractor.extract(request);
            JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(jsonMapper, body.toString());
            session.handle(message).contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext)).block();
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            LOGGER.error("Error processing message: {}", e.getMessage());
            try {
                response.setContentType(APPLICATION_JSON);
                response.setCharacterEncoding(UTF_8);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                String jsonError = jsonMapper.writeValueAsString(McpError.of(e.getMessage()));
                PrintWriter writer = response.getWriter();
                writer.write(jsonError);
                writer.flush();
            } catch (IOException ex) {
                LOGGER.error("Failed to send error response: {}", ex.getMessage());
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing message");
            }
        }
    }

    @Override
    public void destroy() {
        this.closeGracefully().block();
        super.destroy();
    }

    private void sendEvent(PrintWriter writer, String eventType, String data) throws IOException {
        writer.write("event: " + eventType + "\n");
        writer.write("data: " + data + "\n\n");
        writer.flush();
        if (writer.checkError()) {
            throw new IOException("Client disconnected");
        }
    }

    private class HttpServletMcpSessionTransport implements McpServerSessionTransport {
        private final String sessionId;
        private final AsyncContext asyncContext;
        private final PrintWriter writer;

        HttpServletMcpSessionTransport(String sessionId, AsyncContext asyncContext, PrintWriter writer) {
            this.sessionId = sessionId;
            this.asyncContext = asyncContext;
            this.writer = writer;
            LOGGER.debug("Session transport {} initialized with SSE writer", sessionId);
        }

        @Override
        public String sessionId() {
            return sessionId;
        }

        @Override
        public Mono<Void> sendMessage(JSONRPCMessage message) {
            return Mono.fromRunnable(() -> {
                try {
                    String jsonText = jsonMapper.writeValueAsString(message);
                    sendEvent(writer, MESSAGE_EVENT_TYPE, jsonText);
                    LOGGER.debug("Message sent to session {}", sessionId);
                } catch (Exception e) {
                    LOGGER.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
                    sessions.remove(sessionId);
                    asyncContext.complete();
                }
            });
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(() -> {
                LOGGER.debug("Closing session transport: {}", sessionId);
                try {
                    sessions.remove(sessionId);
                    asyncContext.complete();
                    LOGGER.debug("Successfully completed async context for session {}", sessionId);
                } catch (Exception e) {
                    LOGGER.warn("Failed to complete async context for session {}: {}", sessionId, e.getMessage());
                }
            });
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
            return jsonMapper.convertValue(data, typeRef);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private McpJsonMapper jsonMapper;
        private String baseUrl = DEFAULT_BASE_URL;
        private String messageEndpoint;
        private String sseEndpoint = DEFAULT_SSE_ENDPOINT;
        private McpTransportContextExtractor<HttpServletRequest> contextExtractor = (serverRequest) -> McpTransportContext.EMPTY;
        private Duration keepAliveInterval;

        public Builder jsonMapper(McpJsonMapper jsonMapper) {
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

        public Builder contextExtractor(McpTransportContextExtractor<HttpServletRequest> contextExtractor) {
            Assert.notNull(contextExtractor, "Context extractor must not be null");
            this.contextExtractor = contextExtractor;
            return this;
        }

        public Builder keepAliveInterval(Duration keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
            return this;
        }

        public HttpServletSseServerTransportProvider build() {
            if (messageEndpoint == null) {
                throw new IllegalStateException("MessageEndpoint must be set");
            }
            return new HttpServletSseServerTransportProvider(jsonMapper == null ? McpJsonMapper.getDefault() : jsonMapper,
                    baseUrl, messageEndpoint, sseEndpoint, keepAliveInterval, contextExtractor);
        }
    }
}

