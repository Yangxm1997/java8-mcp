package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.transport;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import top.yangxm.ai.mcp.commons.json.JsonMapper;
import top.yangxm.ai.mcp.commons.json.TypeRef;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;
import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.exception.McpError;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCMessage;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerSession;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerSessionFactory;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerSessionTransport;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerSessionTransportProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

@SuppressWarnings("unused")
public class StdioServerTransportProvider implements McpServerSessionTransportProvider {
    private static final Logger logger = LoggerFactoryHolder.getLogger(StdioServerTransportProvider.class);
    private final JsonMapper jsonMapper;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private McpServerSession session;
    private final AtomicBoolean isClosing = new AtomicBoolean(false);
    private final Sinks.One<Void> inboundReady = Sinks.one();

    public StdioServerTransportProvider() {
        this(JsonMapper.getDefault(), System.in, System.out);
    }

    public StdioServerTransportProvider(JsonMapper jsonMapper, InputStream inputStream, OutputStream outputStream) {
        Assert.notNull(jsonMapper, "The JsonMapper can not be null");
        Assert.notNull(inputStream, "The InputStream can not be null");
        Assert.notNull(outputStream, "The OutputStream can not be null");

        this.jsonMapper = jsonMapper;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    @Override
    public void setSessionFactory(McpServerSessionFactory sessionFactory) {
        StdioMcpSessionTransport transport = new StdioMcpSessionTransport();
        this.session = sessionFactory.create(transport);
        transport.initProcessing();
    }

    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        if (this.session == null) {
            return Mono.error(McpError.of("No session to close"));
        }
        return this.session.sendNotification(method, params)
                .doOnError(e -> logger.error("Failed to send notification: {}", e.getMessage()));
    }

    @Override
    public Mono<Void> closeGracefully() {
        if (this.session == null) {
            return Mono.empty();
        }
        return this.session.closeGracefully();
    }

    private class StdioMcpSessionTransport implements McpServerSessionTransport {
        private final String sessionId;
        private final Sinks.Many<JSONRPCMessage> inboundSink;
        private final Sinks.Many<JSONRPCMessage> outboundSink;
        private final AtomicBoolean isStarted = new AtomicBoolean(false);
        private final Scheduler inboundScheduler;
        private final Scheduler outboundScheduler;
        private final Sinks.One<Void> outboundReady = Sinks.one();

        public StdioMcpSessionTransport() {
            this.sessionId = UUID.randomUUID().toString();
            this.inboundSink = Sinks.many().unicast().onBackpressureBuffer();
            this.outboundSink = Sinks.many().unicast().onBackpressureBuffer();
            this.inboundScheduler = Schedulers.fromExecutorService(Executors.newSingleThreadExecutor(), "stdio-inbound");
            this.outboundScheduler = Schedulers.fromExecutorService(Executors.newSingleThreadExecutor(), "stdio-outbound");
        }

        @Override
        public String sessionId() {
            return sessionId;
        }

        @Override
        public Mono<Void> sendMessage(JSONRPCMessage message) {
            return Mono.zip(inboundReady.asMono(), outboundReady.asMono()).then(Mono.defer(() -> {
                if (outboundSink.tryEmitNext(message).isSuccess()) {
                    return Mono.empty();
                } else {
                    return Mono.error(new RuntimeException("Failed to enqueue message"));
                }
            }));
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(() -> {
                isClosing.set(true);
                logger.debug("Session transport closing gracefully");
                inboundSink.tryEmitComplete();
            });
        }

        @Override
        public void close() {
            isClosing.set(true);
            logger.debug("Session transport closed");
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
            return jsonMapper.convertValue(data, typeRef);
        }

        private void initProcessing() {
            handleIncomingMessages();
            startInboundProcessing();
            startOutboundProcessing();
        }

        private void handleIncomingMessages() {
            this.inboundSink.asFlux().flatMap(message -> session.handle(message)).doOnTerminate(() -> {
                this.outboundSink.tryEmitComplete();
                this.inboundScheduler.dispose();
            }).subscribe();
        }

        private void startInboundProcessing() {
            if (isStarted.compareAndSet(false, true)) {
                this.inboundScheduler.schedule(() -> {
                    inboundReady.tryEmitValue(null);
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new InputStreamReader(inputStream));
                        while (!isClosing.get()) {
                            try {
                                String line = reader.readLine();
                                if (line == null || isClosing.get()) {
                                    break;
                                }

                                logger.debug("Received JSON message: {}", line);

                                try {
                                    JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(jsonMapper, line);
                                    if (!this.inboundSink.tryEmitNext(message).isSuccess()) {
                                        break;
                                    }
                                } catch (Exception e) {
                                    logIfNotClosing("Error processing inbound message", e);
                                    break;
                                }
                            } catch (IOException e) {
                                logIfNotClosing("Error reading from stdin", e);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        logIfNotClosing("Error in inbound processing", e);
                    } finally {
                        isClosing.set(true);
                        if (session != null) {
                            session.close();
                        }
                        inboundSink.tryEmitComplete();
                    }
                });
            }
        }

        private void startOutboundProcessing() {
            Function<Flux<JSONRPCMessage>, Flux<JSONRPCMessage>> outboundConsumer = messages -> messages // @formatter:off
                    .doOnSubscribe(subscription -> outboundReady.tryEmitValue(null))
                    .publishOn(outboundScheduler)
                    .handle((message, sink) -> {
                        if (message != null && !isClosing.get()) {
                            try {
                                String jsonMessage = jsonMapper.writeValueAsString(message);
                                jsonMessage = jsonMessage.replace("\r\n", "\\n")
                                        .replace("\n", "\\n")
                                        .replace("\r", "\\n");

                                synchronized (outputStream) {
                                    outputStream.write(jsonMessage.getBytes(StandardCharsets.UTF_8));
                                    outputStream.write("\n".getBytes(StandardCharsets.UTF_8));
                                    outputStream.flush();
                                }
                                sink.next(message);
                            }
                            catch (IOException e) {
                                if (!isClosing.get()) {
                                    logger.error("Error writing message", e);
                                    sink.error(new RuntimeException(e));
                                }
                                else {
                                    logger.debug("Stream closed during shutdown", e);
                                }
                            }
                        }
                        else if (isClosing.get()) {
                            sink.complete();
                        }
                    })
                    .doOnComplete(() -> {
                        isClosing.set(true);
                        outboundScheduler.dispose();
                    })
                    .doOnError(e -> {
                        if (!isClosing.get()) {
                            logger.error("Error in outbound processing", e);
                            isClosing.set(true);
                            outboundScheduler.dispose();
                        }
                    })
                    .map(msg -> (JSONRPCMessage) msg);

            outboundConsumer.apply(outboundSink.asFlux()).subscribe();
        }

        private void logIfNotClosing(String message, Exception e) {
            if (!isClosing.get()) {
                logger.error(message, e);
            }
        }
    }
}
