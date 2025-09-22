package top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.spring;

import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.complete.AsyncMcpCompleteProvider;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.complete.AsyncStatelessMcpCompleteProvider;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.prompt.AsyncMcpPromptProvider;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.prompt.AsyncStatelessMcpPromptProvider;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.resource.AsyncMcpResourceProvider;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.resource.AsyncStatelessMcpResourceProvider;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.tool.AsyncMcpToolProvider;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.tool.AsyncStatelessMcpToolProvider;

import java.lang.reflect.Method;
import java.util.List;

@SuppressWarnings("unused")
public class AsyncMcpAnnotationProviders {

    private AsyncMcpAnnotationProviders() {
    }

    // TOOLS
    public static List<McpServerFeatures.AsyncToolSpec> toolSpecs(List<Object> toolObjects) {
        return new SpringAiAsyncMcpToolProvider(toolObjects).getToolSpecs();
    }

    public static List<McpStatelessServerFeatures.AsyncToolSpec> statelessToolSpecs(List<Object> toolObjects) {
        return new SpringAiAsyncStatelessMcpToolProvider(toolObjects).getToolSpecs();
    }

    // RESOURCE
    public static List<McpServerFeatures.AsyncResourceSpec> resourceSpecs(List<Object> resourceObjects) {
        return new SpringAiAsyncMcpResourceProvider(resourceObjects).getResourceSpecs();
    }

    public static List<McpStatelessServerFeatures.AsyncResourceSpec> statelessResourceSpecs(List<Object> resourceObjects) {
        return new SpringAiAsyncStatelessMcpResourceProvider(resourceObjects).getResourceSpecs();
    }

    // PROMPT
    public static List<McpServerFeatures.AsyncPromptSpec> promptSpecs(List<Object> promptObjects) {
        return new SpringAiAsyncMcpPromptProvider(promptObjects).getPromptSpecs();
    }

    public static List<McpStatelessServerFeatures.AsyncPromptSpec> statelessPromptSpecs(List<Object> promptObjects) {
        return new SpringAiAsyncStatelessMcpPromptProvider(promptObjects).getPromptSpecs();
    }

    // COMPLETE
    public static List<McpServerFeatures.AsyncCompletionSpec> completeSpecs(List<Object> completeObjects) {
        return new SpringAiAsyncMcpCompleteProvider(completeObjects).getCompleteSpecs();
    }

    public static List<McpStatelessServerFeatures.AsyncCompletionSpec> statelessCompleteSpecs(List<Object> completeObjects) {
        return new SpringAiAsyncStatelessMcpCompleteProvider(completeObjects).getCompleteSpecs();
    }


    // TOOL
    private final static class SpringAiAsyncMcpToolProvider extends AsyncMcpToolProvider {
        private SpringAiAsyncMcpToolProvider(List<Object> toolObjects) {
            super(toolObjects);
        }

        @Override
        protected Method[] doGetClassMethods(Object bean) {
            return AnnotationProviderUtil.beanMethods(bean);
        }
    }

    private final static class SpringAiAsyncStatelessMcpToolProvider extends AsyncStatelessMcpToolProvider {
        private SpringAiAsyncStatelessMcpToolProvider(List<Object> toolObjects) {
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

    private final static class SpringAiAsyncStatelessMcpResourceProvider extends AsyncStatelessMcpResourceProvider {
        private SpringAiAsyncStatelessMcpResourceProvider(List<Object> resourceObjects) {
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

    private final static class SpringAiAsyncStatelessMcpPromptProvider extends AsyncStatelessMcpPromptProvider {
        private SpringAiAsyncStatelessMcpPromptProvider(List<Object> promptObjects) {
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

    private final static class SpringAiAsyncStatelessMcpCompleteProvider extends AsyncStatelessMcpCompleteProvider {
        private SpringAiAsyncStatelessMcpCompleteProvider(List<Object> completeObjects) {
            super(completeObjects);
        }

        @Override
        protected Method[] doGetClassMethods(Object bean) {
            return AnnotationProviderUtil.beanMethods(bean);
        }
    }
}
