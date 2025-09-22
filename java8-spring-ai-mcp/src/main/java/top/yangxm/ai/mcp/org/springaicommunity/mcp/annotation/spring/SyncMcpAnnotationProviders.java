package top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.spring;

import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.complete.SyncMcpCompleteProvider;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.complete.SyncStatelessMcpCompleteProvider;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.prompt.SyncMcpPromptProvider;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.prompt.SyncStatelessMcpPromptProvider;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.resource.SyncMcpResourceProvider;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.resource.SyncStatelessMcpResourceProvider;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.tool.SyncMcpToolProvider;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.tool.SyncStatelessMcpToolProvider;

import java.lang.reflect.Method;
import java.util.List;

@SuppressWarnings("unused")
public final class SyncMcpAnnotationProviders {
    private SyncMcpAnnotationProviders() {
    }

    // TOOLS
    public static List<McpServerFeatures.SyncToolSpec> toolSpecs(List<Object> toolObjects) {
        return new SpringAiSyncMcpToolProvider(toolObjects).getToolSpecs();
    }

    public static List<McpStatelessServerFeatures.SyncToolSpec> statelessToolSpecs(List<Object> toolObjects) {
        return new SpringAiSyncStatelessMcpToolProvider(toolObjects).getToolSpecs();
    }

    // RESOURCE
    public static List<McpServerFeatures.SyncResourceSpec> resourceSpecs(List<Object> resourceObjects) {
        return new SpringAiSyncMcpResourceProvider(resourceObjects).getResourceSpecs();
    }

    // RESOURCE
    public static List<McpStatelessServerFeatures.SyncResourceSpec> statelessResourceSpecs(List<Object> resourceObjects) {
        return new SpringAiSyncStatelessMcpResourceProvider(resourceObjects).getResourceSpecs();
    }

    // PROMPT
    public static List<McpServerFeatures.SyncPromptSpec> promptSpecs(List<Object> promptObjects) {
        return new SpringAiSyncMcpPromptProvider(promptObjects).getPromptSpecs();
    }

    public static List<McpStatelessServerFeatures.SyncPromptSpec> statelessPromptSpecs(List<Object> promptObjects) {
        return new SpringAiSyncStatelessMcpPromptProvider(promptObjects).getPromptSpecs();
    }

    // COMPLETE
    public static List<McpServerFeatures.SyncCompletionSpec> completeSpecs(List<Object> completeObjects) {
        return new SpringAiSyncMcpCompleteProvider(completeObjects).getCompleteSpecs();
    }

    public static List<McpStatelessServerFeatures.SyncCompletionSpec> statelessCompleteSpecs(List<Object> completeObjects) {
        return new SpringAiSyncStatelessMcpCompleteProvider(completeObjects).getCompleteSpecs();
    }

    // TOOL
    private final static class SpringAiSyncMcpToolProvider extends SyncMcpToolProvider {
        private SpringAiSyncMcpToolProvider(List<Object> toolObjects) {
            super(toolObjects);
        }

        @Override
        protected Method[] doGetClassMethods(Object bean) {
            return AnnotationProviderUtil.beanMethods(bean);
        }
    }

    private final static class SpringAiSyncStatelessMcpToolProvider extends SyncStatelessMcpToolProvider {
        private SpringAiSyncStatelessMcpToolProvider(List<Object> toolObjects) {
            super(toolObjects);
        }

        @Override
        protected Method[] doGetClassMethods(Object bean) {
            return AnnotationProviderUtil.beanMethods(bean);
        }
    }

    // RESOURCE
    private final static class SpringAiSyncMcpResourceProvider extends SyncMcpResourceProvider {
        private SpringAiSyncMcpResourceProvider(List<Object> resourceObjects) {
            super(resourceObjects);
        }

        @Override
        protected Method[] doGetClassMethods(Object bean) {
            return AnnotationProviderUtil.beanMethods(bean);
        }
    }

    private final static class SpringAiSyncStatelessMcpResourceProvider extends SyncStatelessMcpResourceProvider {
        private SpringAiSyncStatelessMcpResourceProvider(List<Object> resourceObjects) {
            super(resourceObjects);
        }

        @Override
        protected Method[] doGetClassMethods(Object bean) {
            return AnnotationProviderUtil.beanMethods(bean);
        }
    }

    // PROMPT
    private final static class SpringAiSyncMcpPromptProvider extends SyncMcpPromptProvider {
        private SpringAiSyncMcpPromptProvider(List<Object> promptObjects) {
            super(promptObjects);
        }

        @Override
        protected Method[] doGetClassMethods(Object bean) {
            return AnnotationProviderUtil.beanMethods(bean);
        }
    }

    private final static class SpringAiSyncStatelessMcpPromptProvider extends SyncStatelessMcpPromptProvider {
        private SpringAiSyncStatelessMcpPromptProvider(List<Object> promptObjects) {
            super(promptObjects);
        }

        @Override
        protected Method[] doGetClassMethods(Object bean) {
            return AnnotationProviderUtil.beanMethods(bean);
        }
    }

    // COMPLETE
    private final static class SpringAiSyncMcpCompleteProvider extends SyncMcpCompleteProvider {
        private SpringAiSyncMcpCompleteProvider(List<Object> completeObjects) {
            super(completeObjects);
        }

        @Override
        protected Method[] doGetClassMethods(Object bean) {
            return AnnotationProviderUtil.beanMethods(bean);
        }
    }

    private final static class SpringAiSyncStatelessMcpCompleteProvider extends SyncStatelessMcpCompleteProvider {
        private SpringAiSyncStatelessMcpCompleteProvider(List<Object> completeObjects) {
            super(completeObjects);
        }

        @Override
        protected Method[] doGetClassMethods(Object bean) {
            return AnnotationProviderUtil.beanMethods(bean);
        }
    }
}
