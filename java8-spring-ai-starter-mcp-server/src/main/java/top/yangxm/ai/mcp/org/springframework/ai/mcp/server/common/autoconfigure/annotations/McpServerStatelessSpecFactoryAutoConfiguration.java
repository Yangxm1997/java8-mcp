package top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.annotations;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures.AsyncPromptSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures.AsyncResourceSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures.AsyncToolSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures.SyncCompletionSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures.SyncPromptSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures.SyncResourceSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures.SyncToolSpec;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpComplete;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpPrompt;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpResource;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpTool;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.spring.AsyncMcpAnnotationProviders;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.spring.SyncMcpAnnotationProviders;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.McpServerStatelessAutoConfiguration;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.McpServerStdioDisabledCondition;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.StatelessToolCallbackConverterAutoConfiguration;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.annotations.McpServerAnnotationScannerAutoConfiguration.ServerMcpAnnotatedBeans;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerAnnotationScannerProperties;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;

import java.util.List;

@SuppressWarnings("unused")
@AutoConfiguration(after = McpServerAnnotationScannerAutoConfiguration.class)
@ConditionalOnProperty(
        prefix = McpServerAnnotationScannerProperties.CONFIG_PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Conditional({
        McpServerStdioDisabledCondition.class,
        McpServerStatelessAutoConfiguration.EnabledStatelessServerCondition.class,
        StatelessToolCallbackConverterAutoConfiguration.ToolCallbackConverterCondition.class
})
public class McpServerStatelessSpecFactoryAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = McpServerProperties.CONFIG_PREFIX,
            name = "type",
            havingValue = "SYNC",
            matchIfMissing = true
    )
    static class SyncStatelessServerSpecConfiguration {
        @Bean
        public List<SyncToolSpec> statelessToolSpecs(ServerMcpAnnotatedBeans beansWithMcpMethodAnnotations) {
            return SyncMcpAnnotationProviders.statelessToolSpecs(beansWithMcpMethodAnnotations.getBeansByAnnotation(McpTool.class));
        }

        @Bean
        public List<SyncResourceSpec> statelessResourceSpecs(ServerMcpAnnotatedBeans beansWithMcpMethodAnnotations) {
            return SyncMcpAnnotationProviders.statelessResourceSpecs(beansWithMcpMethodAnnotations.getBeansByAnnotation(McpResource.class));
        }

        @Bean
        public List<SyncPromptSpec> statelessPromptSpecs(ServerMcpAnnotatedBeans beansWithMcpMethodAnnotations) {
            return SyncMcpAnnotationProviders.statelessPromptSpecs(beansWithMcpMethodAnnotations.getBeansByAnnotation(McpPrompt.class));
        }

        @Bean
        public List<SyncCompletionSpec> statelessCompleteSpecs(ServerMcpAnnotatedBeans beansWithMcpMethodAnnotations) {
            return SyncMcpAnnotationProviders.statelessCompleteSpecs(beansWithMcpMethodAnnotations.getBeansByAnnotation(McpComplete.class));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = McpServerProperties.CONFIG_PREFIX,
            name = "type",
            havingValue = "ASYNC"
    )
    static class AsyncStatelessServerSpecConfiguration {
        @Bean
        public List<AsyncToolSpec> statelessToolSpecs(ServerMcpAnnotatedBeans beansWithMcpMethodAnnotations) {
            return AsyncMcpAnnotationProviders.statelessToolSpecs(beansWithMcpMethodAnnotations.getBeansByAnnotation(McpTool.class));
        }

        @Bean
        public List<AsyncResourceSpec> statelessResourceSpecs(ServerMcpAnnotatedBeans beansWithMcpMethodAnnotations) {
            return AsyncMcpAnnotationProviders.statelessResourceSpecs(beansWithMcpMethodAnnotations.getBeansByAnnotation(McpResource.class));
        }

        @Bean
        public List<AsyncPromptSpec> statelessPromptSpecs(ServerMcpAnnotatedBeans beansWithMcpMethodAnnotations) {
            return AsyncMcpAnnotationProviders.statelessPromptSpecs(beansWithMcpMethodAnnotations.getBeansByAnnotation(McpPrompt.class));
        }

        @Bean
        public List<McpStatelessServerFeatures.AsyncCompletionSpec> statelessCompleteSpecs(ServerMcpAnnotatedBeans beansWithMcpMethodAnnotations) {
            return AsyncMcpAnnotationProviders.statelessCompleteSpecs(beansWithMcpMethodAnnotations.getBeansByAnnotation(McpComplete.class));
        }
    }
}
