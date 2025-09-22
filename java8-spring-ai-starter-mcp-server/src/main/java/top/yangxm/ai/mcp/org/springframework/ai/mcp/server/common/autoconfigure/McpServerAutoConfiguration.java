package top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.support.StandardServletEnvironment;
import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Implementation;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Root;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ServerCapabilities;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpAsyncServer;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpAsyncServerExchange;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncCompletionSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncPromptSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncResourceSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncToolSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.SyncCompletionSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.SyncPromptSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.SyncResourceSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.SyncToolSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerTransportProvider;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerTransportProviderBase;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStreamableServerTransportProvider;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpSyncServer;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpSyncServerExchange;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.transport.StdioServerTransportProvider;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerChangeNotificationProperties;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@AutoConfiguration(afterName = {
        "top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.ToolCallbackConverterAutoConfiguration",
        "top.yangxm.ai.mcp.org.springframework.ai.mcp.server.autoconfigure.McpServerSseHttpServletAutoConfiguration",
        "top.yangxm.ai.mcp.org.springframework.ai.mcp.server.autoconfigure.McpServerSseWebFluxAutoConfiguration",
        "top.yangxm.ai.mcp.org.springframework.ai.mcp.server.autoconfigure.McpServerSseWebMvcAutoConfiguration"
})
@ConditionalOnClass({McpSchema.class})
@EnableConfigurationProperties({McpServerProperties.class, McpServerChangeNotificationProperties.class})
@ConditionalOnProperty(
        prefix = McpServerProperties.CONFIG_PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Conditional(McpServerAutoConfiguration.NonStatelessServerCondition.class)
public class McpServerAutoConfiguration {
    private static final Logger logger = LoggerFactoryHolder.getLogger(McpServerAutoConfiguration.class);

    public McpServerAutoConfiguration(McpServerProperties serverProperties) {
        logger.info(serverProperties.toString());
    }

    @Bean
    @ConditionalOnMissingBean
    public McpServerTransportProviderBase stdioServerTransport() {
        return new StdioServerTransportProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public ServerCapabilities.Builder capabilitiesBuilder() {
        return ServerCapabilities.builder();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = McpServerProperties.CONFIG_PREFIX,
            name = "type",
            havingValue = "SYNC",
            matchIfMissing = true
    )
    public McpSyncServer mcpSyncServer(McpServerTransportProviderBase transportProvider,
                                       ServerCapabilities.Builder capabilitiesBuilder,
                                       McpServerProperties serverProperties,
                                       McpServerChangeNotificationProperties changeNotificationProperties,
                                       ObjectProvider<List<SyncToolSpec>> tools,
                                       ObjectProvider<List<SyncResourceSpec>> resources,
                                       ObjectProvider<List<SyncPromptSpec>> prompts,
                                       ObjectProvider<List<SyncCompletionSpec>> completions,
                                       ObjectProvider<BiConsumer<McpSyncServerExchange, List<Root>>> rootsChangeConsumers,
                                       Environment environment) {
        Implementation serverInfo = new Implementation(serverProperties.getName(), serverProperties.getVersion());
        McpSyncServer.Builder serverBuilder = McpSyncServer.builder();
        serverBuilder.serverInfo(serverInfo);

        // Tools
        if (serverProperties.getCapabilities().isTool()) {
            boolean notification = changeNotificationProperties.isToolChangeNotification();
            logger.info("[Sync] Enable tools capabilities, notification: {}", notification);
            capabilitiesBuilder.tools(notification);
            List<SyncToolSpec> toolSpecs = tools.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(toolSpecs)) {
                serverBuilder.toolSpecs(toolSpecs);
                logger.info("[Sync] Registered tools: {}", toolSpecs.size());
            }
        }

        // Resources
        if (serverProperties.getCapabilities().isResource()) {
            boolean notification = changeNotificationProperties.isResourceChangeNotification();
            logger.info("[Sync] Enable resources capabilities, notification: {}", notification);
            capabilitiesBuilder.resources(false, notification);
            List<SyncResourceSpec> resourceSpecs = resources.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(resourceSpecs)) {
                serverBuilder.resourceSpecs(resourceSpecs);
                logger.info("[Sync] Registered resources: {}", resourceSpecs.size());
            }
        }

        // Prompts
        if (serverProperties.getCapabilities().isPrompt()) {
            boolean notification = changeNotificationProperties.isPromptChangeNotification();
            logger.info("[Sync] Enable prompts capabilities, notification: {}", notification);
            capabilitiesBuilder.prompts(notification);
            List<SyncPromptSpec> promptSpecs = prompts.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(promptSpecs)) {
                serverBuilder.promptSpecs(promptSpecs);
                logger.info("[Sync] Registered prompts: {}", promptSpecs.size());
            }
        }

        // Completions
        if (serverProperties.getCapabilities().isCompletion()) {
            logger.info("[Sync] Enable completions capabilities");
            capabilitiesBuilder.completions();
            List<SyncCompletionSpec> completionSpecs = completions.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(completionSpecs)) {
                serverBuilder.completionSpecs(completionSpecs);
                logger.info("[Sync] Registered completions: {}", completionSpecs.size());
            }
        }

        rootsChangeConsumers.ifAvailable(consumer -> {
            serverBuilder.rootsChangeConsumers(consumer);
            logger.info("[Sync] Registered roots change consumer");
        });

        serverBuilder.serverCapabilities(capabilitiesBuilder.build());
        serverBuilder.instructions(serverProperties.getInstructions());
        serverBuilder.requestTimeout(serverProperties.getRequestTimeout());
        if (environment instanceof StandardServletEnvironment) {
            serverBuilder.immediateExecution(true);
        }

        if (transportProvider instanceof McpStreamableServerTransportProvider) {
            return serverBuilder.buildStreamableSessionMcpServer((McpStreamableServerTransportProvider) transportProvider);
        } else {
            return serverBuilder.buildSingleSessionMcpServer((McpServerTransportProvider) transportProvider);
        }
    }

    @Bean
    @ConditionalOnProperty(
            prefix = McpServerProperties.CONFIG_PREFIX,
            name = "type",
            havingValue = "ASYNC"
    )
    public McpAsyncServer mcpAsyncServer(McpServerTransportProviderBase transportProvider,
                                         ServerCapabilities.Builder capabilitiesBuilder,
                                         McpServerProperties serverProperties,
                                         McpServerChangeNotificationProperties changeNotificationProperties,
                                         ObjectProvider<List<AsyncToolSpec>> tools,
                                         ObjectProvider<List<AsyncResourceSpec>> resources,
                                         ObjectProvider<List<AsyncPromptSpec>> prompts,
                                         ObjectProvider<List<AsyncCompletionSpec>> completions,
                                         ObjectProvider<BiConsumer<McpAsyncServerExchange, List<McpSchema.Root>>> rootsChangeConsumer) {
        Implementation serverInfo = new Implementation(serverProperties.getName(), serverProperties.getVersion());
        McpAsyncServer.Builder serverBuilder = McpAsyncServer.builder();
        serverBuilder.serverInfo(serverInfo);

        // Tools
        if (serverProperties.getCapabilities().isTool()) {
            boolean notification = changeNotificationProperties.isToolChangeNotification();
            logger.info("[Async] Enable tools capabilities, notification: {}", notification);
            capabilitiesBuilder.tools(notification);
            List<AsyncToolSpec> toolSpecs = tools.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(toolSpecs)) {
                serverBuilder.toolSpecs(toolSpecs);
                logger.info("[Async] Registered tools: {}", toolSpecs.size());
            }
        }

        // Resources
        if (serverProperties.getCapabilities().isResource()) {
            boolean notification = changeNotificationProperties.isResourceChangeNotification();
            logger.info("[Async] Enable resources capabilities, notification: {}", notification);
            capabilitiesBuilder.resources(false, notification);
            List<AsyncResourceSpec> resourceSpecs = resources.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(resourceSpecs)) {
                serverBuilder.resourceSpecs(resourceSpecs);
                logger.info("[Async] Registered resources: {}", resourceSpecs.size());
            }
        }

        // Prompts
        if (serverProperties.getCapabilities().isPrompt()) {
            boolean notification = changeNotificationProperties.isPromptChangeNotification();
            logger.info("[Async] Enable prompts capabilities, notification: {}", notification);
            capabilitiesBuilder.prompts(notification);
            List<AsyncPromptSpec> promptSpecs = prompts.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(promptSpecs)) {
                serverBuilder.promptSpecs(promptSpecs);
                logger.info("[Async] Registered prompts: {}", promptSpecs.size());
            }
        }

        // Completions
        if (serverProperties.getCapabilities().isCompletion()) {
            logger.info("[Async] Enable completions capabilities");
            capabilitiesBuilder.completions();
            List<AsyncCompletionSpec> completionSpecs = completions.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(completionSpecs)) {
                serverBuilder.completionSpecs(completionSpecs);
                logger.info("[Async] Registered completions: {}", completionSpecs.size());
            }
        }

        rootsChangeConsumer.ifAvailable(consumer -> {
            BiFunction<McpAsyncServerExchange, List<Root>, Mono<Void>> asyncConsumer = (exchange, roots) -> {
                consumer.accept(exchange, roots);
                return Mono.empty();
            };
            serverBuilder.rootsChangeConsumers(asyncConsumer);
            logger.info("[Async] Registered roots change consumer");
        });

        serverBuilder.serverCapabilities(capabilitiesBuilder.build());
        serverBuilder.instructions(serverProperties.getInstructions());
        serverBuilder.requestTimeout(serverProperties.getRequestTimeout());

        if (transportProvider instanceof McpStreamableServerTransportProvider) {
            return serverBuilder.buildStreamableSessionMcpServer((McpStreamableServerTransportProvider) transportProvider);
        } else {
            return serverBuilder.buildSingleSessionMcpServer((McpServerTransportProvider) transportProvider);
        }
    }


    public static class NonStatelessServerCondition extends AnyNestedCondition {
        public NonStatelessServerCondition() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty(
                prefix = McpServerProperties.CONFIG_PREFIX,
                name = "protocol",
                havingValue = "SSE",
                matchIfMissing = true
        )
        static class SseEnabledCondition {
        }

        @ConditionalOnProperty(
                prefix = McpServerProperties.CONFIG_PREFIX,
                name = "protocol",
                havingValue = "STREAMABLE"
        )
        static class StreamableEnabledCondition {
        }
    }

    public static class EnabledSseServerCondition extends AllNestedConditions {
        public EnabledSseServerCondition() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty(
                prefix = McpServerProperties.CONFIG_PREFIX,
                name = "enabled",
                havingValue = "true",
                matchIfMissing = true
        )
        static class McpServerEnabledCondition {
        }

        @ConditionalOnProperty(prefix =
                McpServerProperties.CONFIG_PREFIX,
                name = "protocol",
                havingValue = "SSE",
                matchIfMissing = true
        )
        static class SseEnabledCondition {
            public SseEnabledCondition() {
                McpServerAutoConfiguration.logger.info("Mcp Server Protocol: SSE");
            }
        }
    }

    public static class EnabledStreamableServerCondition extends AllNestedConditions {
        public EnabledStreamableServerCondition() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty(
                prefix = McpServerProperties.CONFIG_PREFIX,
                name = "enabled",
                havingValue = "true",
                matchIfMissing = true
        )
        static class McpServerEnabledCondition {
        }

        @ConditionalOnProperty(
                prefix = McpServerProperties.CONFIG_PREFIX,
                name = "protocol",
                havingValue = "STREAMABLE"
        )
        static class StreamableEnabledCondition {
            public StreamableEnabledCondition() {
                McpServerAutoConfiguration.logger.info("Mcp Server Protocol: STREAMABLE");
            }
        }
    }
}
