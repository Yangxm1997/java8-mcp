package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.transport;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.json.JsonException;
import top.yangxm.ai.mcp.commons.json.JsonMapper;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;
import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransportContext;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.exception.McpError;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCMessage;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCNotification;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCResponse;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerHandler;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerTransport;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpTransportContextExtractor;

import java.util.List;

@SuppressWarnings("unused")
public class WebMvcStatelessServerTransport implements McpStatelessServerTransport {
    private static final Logger logger = LoggerFactoryHolder.getLogger(WebMvcStatelessServerTransport.class);

    private final JsonMapper jsonMapper;
    private final String messageEndpoint;
    private final RouterFunction<ServerResponse> routerFunction;
    private final McpTransportContextExtractor<ServerRequest> contextExtractor;
    private McpStatelessServerHandler mcpHandler;
    private volatile boolean isClosing = false;

    private WebMvcStatelessServerTransport(JsonMapper jsonMapper, String messageEndpoint,
                                           McpTransportContextExtractor<ServerRequest> contextExtractor) {
        Assert.notNull(jsonMapper, "jsonMapper must not be null");
        Assert.notNull(messageEndpoint, "messageEndpoint must not be null");
        Assert.notNull(contextExtractor, "contextExtractor must not be null");

        this.jsonMapper = jsonMapper;
        this.messageEndpoint = messageEndpoint;
        this.contextExtractor = contextExtractor;
        this.routerFunction = RouterFunctions.route()
                .GET(this.messageEndpoint, this::handleGet)
                .POST(this.messageEndpoint, this::handlePost)
                .build();

        logger.debug("WebMVC STATELESS transport provider initialized with messageEndpoint: {}", messageEndpoint);
    }

    public String messageEndpoint() {
        return messageEndpoint;
    }

    @Override
    public void setMcpHandler(McpStatelessServerHandler mcpHandler) {
        this.mcpHandler = mcpHandler;
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> this.isClosing = true);
    }

    private ServerResponse handleGet(ServerRequest request) {
        return ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    private ServerResponse handlePost(ServerRequest request) {
        if (isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
        }
        McpTransportContext transportContext = this.contextExtractor.extract(request);

        List<MediaType> acceptHeaders = request.headers().asHttpHeaders().getAccept();
        if (!(acceptHeaders.contains(MediaType.APPLICATION_JSON)
                && acceptHeaders.contains(MediaType.TEXT_EVENT_STREAM))) {
            return ServerResponse.badRequest().build();
        }

        try {
            String body = request.body(String.class);
            JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(jsonMapper, body);
            if (message instanceof JSONRPCRequest) {
                try {
                    JSONRPCRequest jsonrpcRequest = (JSONRPCRequest) message;
                    JSONRPCResponse jsonrpcResponse = this.mcpHandler
                            .handleRequest(transportContext, jsonrpcRequest)
                            .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                            .block();
                    if (jsonrpcResponse == null) {
                        throw new RuntimeException("Response is null");
                    }
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(jsonrpcResponse);
                } catch (Exception e) {
                    logger.error("Failed to handle request: {}", e.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(McpError.of("Failed to handle request: " + e.getMessage()));
                }
            } else if (message instanceof JSONRPCNotification) {
                try {
                    JSONRPCNotification jsonrpcNotification = (JSONRPCNotification) message;
                    this.mcpHandler.handleNotification(transportContext, jsonrpcNotification)
                            .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                            .block();
                    return ServerResponse.accepted().build();
                } catch (Exception e) {
                    logger.error("Failed to handle notification: {}", e.getMessage());
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(McpError.of("Failed to handle notification: " + e.getMessage()));
                }
            } else {
                return ServerResponse.badRequest()
                        .body(McpError.of("The server accepts either requests or notifications"));
            }
        } catch (JsonException | IllegalArgumentException e) {
            logger.error("Failed to deserialize message: {}", e.getMessage());
            return ServerResponse.badRequest().body(McpError.of("Invalid message format"));
        } catch (Exception e) {
            logger.error("Unexpected error handling message: {}", e.getMessage());
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(McpError.of("Unexpected error: " + e.getMessage()));
        }
    }

    public RouterFunction<ServerResponse> getRouterFunction() {
        return this.routerFunction;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private JsonMapper jsonMapper = JsonMapper.getDefault();
        private String messageEndpoint = McpTransportConst.DEFAULT_STATELESS_MESSAGE_ENDPOINT;
        private McpTransportContextExtractor<ServerRequest> contextExtractor = (serverRequest) -> McpTransportContext.EMPTY;

        private Builder() {
        }

        public Builder jsonMapper(JsonMapper jsonMapper) {
            Assert.notNull(jsonMapper, "jsonMapper must not be null");
            this.jsonMapper = jsonMapper;
            return this;
        }

        public Builder messageEndpoint(String messageEndpoint) {
            Assert.notNull(messageEndpoint, "Message Endpoint must not be null");
            this.messageEndpoint = messageEndpoint;
            return this;
        }

        public Builder contextExtractor(McpTransportContextExtractor<ServerRequest> contextExtractor) {
            Assert.notNull(contextExtractor, "Context extractor must not be null");
            this.contextExtractor = contextExtractor;
            return this;
        }

        public WebMvcStatelessServerTransport build() {
            Assert.notNull(this.messageEndpoint, "Message endpoint must be set");
            return new WebMvcStatelessServerTransport(jsonMapper, messageEndpoint, contextExtractor);
        }
    }
}
