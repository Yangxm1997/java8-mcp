package top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.support.StandardServletEnvironment;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Implementation;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ServerCapabilities;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessAsyncServer;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures.SyncCompletionSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures.SyncPromptSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures.SyncResourceSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures.SyncToolSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerTransport;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessSyncServer;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@AutoConfiguration(afterName = {
        "org.springframework.ai.mcp.server.common.autoconfigure.StatelessToolCallbackConverterAutoConfiguration",
        "org.springframework.ai.mcp.server.autoconfigure.McpServerStatelessWebFluxAutoConfiguration",
        "org.springframework.ai.mcp.server.autoconfigure.McpServerStatelessWebMvcAutoConfiguration"})
@ConditionalOnClass({McpSchema.class})
@EnableConfigurationProperties(McpServerProperties.class)
@Conditional({
        McpServerStdioDisabledCondition.class,
        McpServerStatelessAutoConfiguration.EnabledStatelessServerCondition.class
})
public class McpServerStatelessAutoConfiguration {
    private static final Logger logger = LoggerFactoryHolder.getLogger(McpServerAutoConfiguration.class);

    public McpServerStatelessAutoConfiguration(McpServerProperties serverProperties) {
        logger.info(serverProperties.toString());
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
    public McpStatelessSyncServer mcpStatelessSyncServer(McpStatelessServerTransport statelessTransport,
                                                         ServerCapabilities.Builder capabilitiesBuilder,
                                                         McpServerProperties serverProperties,
                                                         ObjectProvider<List<SyncToolSpec>> tools,
                                                         ObjectProvider<List<SyncResourceSpec>> resources,
                                                         ObjectProvider<List<SyncPromptSpec>> prompts,
                                                         ObjectProvider<List<SyncCompletionSpec>> completions,
                                                         Environment environment) {
        Implementation serverInfo = new Implementation(serverProperties.getName(), serverProperties.getVersion());
        McpStatelessSyncServer.Builder serverBuilder = McpStatelessSyncServer.builder();
        serverBuilder.serverInfo(serverInfo);

        // Tools
        if (serverProperties.getCapabilities().isTool()) {
            capabilitiesBuilder.tools(false);
            List<SyncToolSpec> toolSpecs = tools.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(toolSpecs)) {
                serverBuilder.toolSpecs(toolSpecs);
                logger.info("[Sync] Registered tools: {}", toolSpecs.size());
            }
        }

        // Resources
        if (serverProperties.getCapabilities().isResource()) {
            capabilitiesBuilder.resources(false, false);
            List<SyncResourceSpec> resourceSpecs = resources.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(resourceSpecs)) {
                serverBuilder.resourceSpecs(resourceSpecs);
                logger.info("[Sync] Registered resources: {}", resourceSpecs.size());
            }
        }

        // Prompts
        if (serverProperties.getCapabilities().isPrompt()) {
            ;
            capabilitiesBuilder.prompts(false);
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

        serverBuilder.serverCapabilities(capabilitiesBuilder.build());
        serverBuilder.instructions(serverProperties.getInstructions());
        serverBuilder.requestTimeout(serverProperties.getRequestTimeout());
        if (environment instanceof StandardServletEnvironment) {
            serverBuilder.immediateExecution(true);
        }

        return serverBuilder.build(statelessTransport);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = McpServerProperties.CONFIG_PREFIX,
            name = "type",
            havingValue = "ASYNC"
    )
    public McpStatelessAsyncServer mcpStatelessAsyncServer(McpStatelessServerTransport statelessTransport,
                                                           ServerCapabilities.Builder capabilitiesBuilder,
                                                           McpServerProperties serverProperties,
                                                           ObjectProvider<List<McpStatelessServerFeatures.AsyncToolSpec>> tools,
                                                           ObjectProvider<List<McpStatelessServerFeatures.AsyncResourceSpec>> resources,
                                                           ObjectProvider<List<McpStatelessServerFeatures.AsyncPromptSpec>> prompts,
                                                           ObjectProvider<List<McpStatelessServerFeatures.AsyncCompletionSpec>> completions) {
        Implementation serverInfo = new Implementation(serverProperties.getName(), serverProperties.getVersion());
        McpStatelessAsyncServer.Builder serverBuilder = McpStatelessAsyncServer.builder();
        serverBuilder.serverInfo(serverInfo);

        // Tools
        if (serverProperties.getCapabilities().isTool()) {
            capabilitiesBuilder.tools(false);
            List<McpStatelessServerFeatures.AsyncToolSpec> toolSpecs = tools.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(toolSpecs)) {
                serverBuilder.toolSpecs(toolSpecs);
                logger.info("[Async] Registered tools: {}", toolSpecs.size());
            }
        }

        // Resources
        if (serverProperties.getCapabilities().isResource()) {
            capabilitiesBuilder.resources(false, false);
            List<McpStatelessServerFeatures.AsyncResourceSpec> resourceSpecs = resources.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(resourceSpecs)) {
                serverBuilder.resourceSpecs(resourceSpecs);
                logger.info("[Async] Registered resources: {}", resourceSpecs.size());
            }
        }

        // Prompts
        if (serverProperties.getCapabilities().isPrompt()) {
            capabilitiesBuilder.prompts(false);
            List<McpStatelessServerFeatures.AsyncPromptSpec> promptSpecs = prompts.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(promptSpecs)) {
                serverBuilder.promptSpecs(promptSpecs);
                logger.info("[Async] Registered prompts: {}", promptSpecs.size());
            }
        }

        // Completions
        if (serverProperties.getCapabilities().isCompletion()) {
            logger.info("[Async] Enable completions capabilities");
            capabilitiesBuilder.completions();
            List<McpStatelessServerFeatures.AsyncCompletionSpec> completionSpecs = completions.stream().flatMap(List::stream).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(completionSpecs)) {
                serverBuilder.completionSpecs(completionSpecs);
                logger.info("[Async] Registered completions: {}", completionSpecs.size());
            }
        }

        serverBuilder.serverCapabilities(capabilitiesBuilder.build());
        serverBuilder.instructions(serverProperties.getInstructions());
        serverBuilder.requestTimeout(serverProperties.getRequestTimeout());

        return serverBuilder.build(statelessTransport);
    }

    public static class EnabledStatelessServerCondition extends AllNestedConditions {
        public EnabledStatelessServerCondition() {
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
                havingValue = "STATELESS"
        )
        static class StatelessEnabledCondition {
        }
    }
}
