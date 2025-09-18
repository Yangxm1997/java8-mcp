package top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.annotations;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncCompletionSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncPromptSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncResourceSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncToolSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.SyncCompletionSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.SyncPromptSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.SyncResourceSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.SyncToolSpec;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpComplete;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpPrompt;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpResource;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpTool;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.spring.AsyncMcpAnnotationProviders;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.spring.SyncMcpAnnotationProviders;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.annotations.McpServerAnnotationScannerAutoConfiguration.ServerMcpAnnotatedBeans;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerAnnotationScannerProperties;
import top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties;

import java.util.List;

@SuppressWarnings("unused")
@AutoConfiguration(after = McpServerAnnotationScannerAutoConfiguration.class)
@ConditionalOnClass(McpTool.class)
@ConditionalOnProperty(
        prefix = McpServerAnnotationScannerProperties.CONFIG_PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Conditional(McpServerAutoConfiguration.NonStatelessServerCondition.class)
public class McpServerSpecFactoryAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = McpServerProperties.CONFIG_PREFIX,
            name = "type",
            havingValue = "SYNC",
            matchIfMissing = true
    )
    static class SyncServerSpecConfiguration {
        @Bean
        public List<SyncToolSpec> toolSpecs(ServerMcpAnnotatedBeans beansWithMcpMethodAnnotations) {
            return SyncMcpAnnotationProviders.toolSpecs(beansWithMcpMethodAnnotations.getBeansByAnnotation(McpTool.class));
        }

        @Bean
        public List<SyncResourceSpec> resourceSpecs(ServerMcpAnnotatedBeans beansWithMcpMethodAnnotations) {
            return SyncMcpAnnotationProviders.resourceSpecs(beansWithMcpMethodAnnotations.getBeansByAnnotation(McpResource.class));
        }

        @Bean
        public List<SyncPromptSpec> promptSpecs(ServerMcpAnnotatedBeans beansWithMcpMethodAnnotations) {
            return SyncMcpAnnotationProviders.promptSpecs(beansWithMcpMethodAnnotations.getBeansByAnnotation(McpPrompt.class));
        }

        @Bean
        public List<SyncCompletionSpec> completeSpecs(ServerMcpAnnotatedBeans beansWithMcpMethodAnnotations) {
            return SyncMcpAnnotationProviders.completeSpecs(beansWithMcpMethodAnnotations.getBeansByAnnotation(McpComplete.class));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = McpServerProperties.CONFIG_PREFIX,
            name = "type",
            havingValue = "ASYNC"
    )
    static class AsyncServerSpecConfiguration {
        @Bean
        public List<AsyncToolSpec> toolSpecs(ServerMcpAnnotatedBeans beansWithMcpMethodAnnotations) {
            return AsyncMcpAnnotationProviders.toolSpecs(beansWithMcpMethodAnnotations.getBeansByAnnotation(McpTool.class));
        }

        @Bean
        public List<AsyncResourceSpec> resourceSpecs(ServerMcpAnnotatedBeans beansWithMcpMethodAnnotations) {
            return AsyncMcpAnnotationProviders.resourceSpecs(beansWithMcpMethodAnnotations.getBeansByAnnotation(McpResource.class));
        }

        @Bean
        public List<AsyncPromptSpec> promptSpecs(ServerMcpAnnotatedBeans beansWithMcpMethodAnnotations) {
            return AsyncMcpAnnotationProviders.promptSpecs(beansWithMcpMethodAnnotations.getBeansByAnnotation(McpPrompt.class));
        }

        @Bean
        public List<AsyncCompletionSpec> completeSpecs(ServerMcpAnnotatedBeans beansWithMcpMethodAnnotations) {
            return AsyncMcpAnnotationProviders.completeSpecs(beansWithMcpMethodAnnotations.getBeansByAnnotation(McpComplete.class));
        }
    }
}
