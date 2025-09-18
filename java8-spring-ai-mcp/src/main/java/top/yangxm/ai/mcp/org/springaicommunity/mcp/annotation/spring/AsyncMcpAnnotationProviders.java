package top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.spring;

import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncCompletionSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncPromptSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncResourceSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncToolSpec;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.complete.AsyncMcpCompleteProvider;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.prompt.AsyncMcpPromptProvider;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.resource.AsyncMcpResourceProvider;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.tool.AsyncMcpToolProvider;

import java.lang.reflect.Method;
import java.util.List;

@SuppressWarnings("unused")
public class AsyncMcpAnnotationProviders {

    private AsyncMcpAnnotationProviders() {
    }

    // TOOLS
    public static List<AsyncToolSpec> toolSpecs(List<Object> toolObjects) {
        return new SpringAiAsyncToolProvider(toolObjects).getToolSpecs();
    }

    // RESOURCE
    public static List<AsyncResourceSpec> resourceSpecs(List<Object> resourceObjects) {
        return new SpringAiAsyncMcpResourceProvider(resourceObjects).getResourceSpecs();
    }

    // PROMPT
    public static List<AsyncPromptSpec> promptSpecs(List<Object> promptObjects) {
        return new SpringAiAsyncMcpPromptProvider(promptObjects).getPromptSpecs();
    }

    // COMPLETE
    public static List<AsyncCompletionSpec> completeSpecs(List<Object> completeObjects) {
        return new SpringAiAsyncMcpCompleteProvider(completeObjects).getCompleteSpecs();
    }


    // TOOL
    private final static class SpringAiAsyncToolProvider extends AsyncMcpToolProvider {
        private SpringAiAsyncToolProvider(List<Object> toolObjects) {
            super(toolObjects);
        }

        @Override
        protected Method[] doGetClassMethods(Object bean) {
            return AnnotationProviderUtil.beanMethods(bean);
        }
    }

    // RESOURCE
    private final static class SpringAiAsyncMcpResourceProvider extends AsyncMcpResourceProvider {
        private SpringAiAsyncMcpResourceProvider(List<Object> resourceObjects) {
            super(resourceObjects);
        }

        @Override
        protected Method[] doGetClassMethods(Object bean) {
            return AnnotationProviderUtil.beanMethods(bean);
        }
    }

    // PROMPT
    private final static class SpringAiAsyncMcpPromptProvider extends AsyncMcpPromptProvider {
        private SpringAiAsyncMcpPromptProvider(List<Object> promptObjects) {
            super(promptObjects);
        }

        @Override
        protected Method[] doGetClassMethods(Object bean) {
            return AnnotationProviderUtil.beanMethods(bean);
        }
    }

    // COMPLETE
    private final static class SpringAiAsyncMcpCompleteProvider extends AsyncMcpCompleteProvider {
        private SpringAiAsyncMcpCompleteProvider(List<Object> completeObjects) {
            super(completeObjects);
        }

        @Override
        protected Method[] doGetClassMethods(Object bean) {
            return AnnotationProviderUtil.beanMethods(bean);
        }
    }
}
