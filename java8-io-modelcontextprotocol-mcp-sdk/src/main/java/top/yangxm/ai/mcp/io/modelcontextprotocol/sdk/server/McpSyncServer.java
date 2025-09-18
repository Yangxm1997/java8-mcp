package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.json.McpJsonMapper;
import top.yangxm.ai.mcp.commons.json.schema.JsonSchemaValidator;
import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpUriTemplateManager;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CompleteReference;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Implementation;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ResourceTemplate;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ResourcesUpdatedNotification;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Root;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ServerCapabilities;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncCompletionSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncPromptSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncResourceSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.AsyncToolSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.SyncCompletionSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.SyncPromptSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.SyncResourceSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpServerFeatures.SyncToolSpec;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@SuppressWarnings("unused")
public class McpSyncServer {
    private final McpAsyncServer asyncServer;
    private final boolean immediateExecution;

    private McpSyncServer(McpAsyncServer asyncServer, boolean immediateExecution) {
        Assert.notNull(asyncServer, "Async server must not be null");
        this.asyncServer = asyncServer;
        this.immediateExecution = immediateExecution;
    }

    public ServerCapabilities serverCapabilities() {
        return this.asyncServer.serverCapabilities();
    }

    public Implementation serverInfo() {
        return this.asyncServer.serverInfo();
    }

    public McpAsyncServer getAsyncServer() {
        return this.asyncServer;
    }

    public void addTool(SyncToolSpec toolSpec) {
        this.asyncServer.addTool(AsyncToolSpec.fromSync(toolSpec, this.immediateExecution)).block();
    }

    public void removeTool(String toolName) {
        this.asyncServer.removeTool(toolName).block();
    }

    public void notifyToolsListChanged() {
        this.asyncServer.notifyToolsListChanged().block();
    }

    public void addResource(SyncResourceSpec resourceSpec) {
        this.asyncServer.addResource(AsyncResourceSpec.fromSync(resourceSpec, this.immediateExecution)).block();
    }

    public void removeResource(String resourceUri) {
        this.asyncServer.removeResource(resourceUri).block();
    }

    public void notifyResourcesListChanged() {
        this.asyncServer.notifyResourcesListChanged().block();
    }

    public void notifyResourcesUpdated(ResourcesUpdatedNotification resourcesUpdatedNotification) {
        this.asyncServer.notifyResourcesUpdated(resourcesUpdatedNotification).block();
    }

    public void addPrompt(SyncPromptSpec promptSpec) {
        this.asyncServer.addPrompt(AsyncPromptSpec.fromSync(promptSpec, this.immediateExecution)).block();
    }

    public void removePrompt(String promptName) {
        this.asyncServer.removePrompt(promptName).block();
    }

    public void notifyPromptsListChanged() {
        this.asyncServer.notifyPromptsListChanged().block();
    }

    public void closeGracefully() {
        this.asyncServer.closeGracefully().block();
    }

    public void close() {
        this.asyncServer.close();
    }

    public static Builder builder() {
        return new Builder();
    }

    public final static class Builder {
        private final McpAsyncServer.Builder asyncBuilder;
        private final List<SyncToolSpec> toolSpecs = new ArrayList<>();
        private final List<SyncResourceSpec> resourceSpecs = new ArrayList<>();
        private final List<SyncPromptSpec> promptSpecs = new ArrayList<>();
        private final List<SyncCompletionSpec> completionSpecs = new ArrayList<>();
        private boolean immediateExecution = false;

        private Builder() {
            this.asyncBuilder = McpAsyncServer.builder();
        }

        public Builder immediateExecution(boolean immediateExecution) {
            this.immediateExecution = immediateExecution;
            return this;
        }

        public Builder jsonMapper(McpJsonMapper jsonMapper) {
            this.asyncBuilder.jsonMapper(jsonMapper);
            return this;
        }

        public Builder uriTemplateManagerFactory(McpUriTemplateManager.Factory uriTemplateManagerFactory) {
            this.asyncBuilder.uriTemplateManagerFactory(uriTemplateManagerFactory);
            return this;
        }

        public Builder jsonSchemaValidator(JsonSchemaValidator jsonSchemaValidator) {
            this.asyncBuilder.jsonSchemaValidator(jsonSchemaValidator);
            return this;
        }

        public Builder serverCapabilities(ServerCapabilities serverCapabilities) {
            this.asyncBuilder.serverCapabilities(serverCapabilities);
            return this;
        }

        public Builder serverInfo(Implementation serverInfo) {
            this.asyncBuilder.serverInfo(serverInfo);
            return this;
        }

        public Builder serverInfo(String name, String version) {
            this.asyncBuilder.serverInfo(name, version);
            return this;
        }

        public Builder instructions(String instructions) {
            this.asyncBuilder.instructions(instructions);
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.asyncBuilder.requestTimeout(requestTimeout);
            return this;
        }

        public Builder toolSpecs(List<SyncToolSpec> toolSpecs) {
            if (toolSpecs != null) {
                Set<String> toolNames = new HashSet<>();
                for (SyncToolSpec toolSpec : this.toolSpecs) {
                    toolNames.add(toolSpec.tool().name());
                }

                for (SyncToolSpec toolSpec : toolSpecs) {
                    if (toolSpec == null) {
                        continue;
                    }
                    final String toolName = toolSpec.tool().name();
                    if (!toolNames.add(toolName)) {
                        throw new IllegalStateException("Duplicate tool name: " + toolName);
                    }
                    this.toolSpecs.add(toolSpec);
                }
            }
            return this;
        }

        public Builder toolSpecs(SyncToolSpec... toolSpecs) {
            return this.toolSpecs(Arrays.asList(toolSpecs));
        }

        public Builder resourceSpecs(List<SyncResourceSpec> resourceSpecs) {
            if (resourceSpecs != null) {
                Set<String> resourceUris = new HashSet<>();
                for (SyncResourceSpec resourceSpec : this.resourceSpecs) {
                    resourceUris.add(resourceSpec.resource().uri());
                }

                for (SyncResourceSpec resourceSpec : resourceSpecs) {
                    if (resourceSpec == null) {
                        continue;
                    }
                    final String uri = resourceSpec.resource().uri();
                    if (!resourceUris.add(uri)) {
                        throw new IllegalStateException("Duplicate resource uri: " + uri);
                    }
                    this.resourceSpecs.add(resourceSpec);
                }
            }
            return this;
        }

        public Builder resourceSpecs(SyncResourceSpec... resourceSpecs) {
            return this.resourceSpecs(Arrays.asList(resourceSpecs));
        }

        public Builder resourceTemplates(List<ResourceTemplate> resourceTemplates) {
            this.asyncBuilder.resourceTemplates(resourceTemplates);
            return this;
        }

        public Builder resourceTemplates(ResourceTemplate... resourceTemplates) {
            return this.resourceTemplates(Arrays.asList(resourceTemplates));
        }

        public Builder promptSpecs(List<SyncPromptSpec> promptSpecs) {
            if (promptSpecs != null) {
                Set<String> promptNames = new HashSet<>();
                for (SyncPromptSpec promptSpec : this.promptSpecs) {
                    promptNames.add(promptSpec.prompt().name());
                }

                for (SyncPromptSpec promptSpec : promptSpecs) {
                    if (promptSpec == null) {
                        continue;
                    }
                    final String promptName = promptSpec.prompt().name();
                    if (!promptNames.add(promptName)) {
                        throw new IllegalStateException("Duplicate prompt name: " + promptName);
                    }
                    this.promptSpecs.add(promptSpec);
                }
            }
            return this;
        }

        public Builder promptSpecs(SyncPromptSpec... promptSpecs) {
            return this.promptSpecs(Arrays.asList(promptSpecs));
        }

        public Builder completionSpecs(List<SyncCompletionSpec> completionSpecs) {
            if (completionSpecs != null) {
                Set<CompleteReference> referenceKeys = new HashSet<>();
                for (SyncCompletionSpec completionSpec : this.completionSpecs) {
                    referenceKeys.add(completionSpec.referenceKey());
                }

                for (SyncCompletionSpec completionSpec : completionSpecs) {
                    if (completionSpec == null) {
                        continue;
                    }
                    if (!referenceKeys.add(completionSpec.referenceKey())) {
                        throw new IllegalStateException("Duplicate completion reference key: " + completionSpec.referenceKey());
                    }
                    this.completionSpecs.add(completionSpec);
                }
            }
            return this;
        }

        public Builder completionSpecs(SyncCompletionSpec... completionSpecs) {
            return this.completionSpecs(Arrays.asList(completionSpecs));
        }

        public Builder rootsChangeConsumers
                (List<BiConsumer<McpSyncServerExchange, List<Root>>> rootsChangeConsumers) {
            if (rootsChangeConsumers != null) {
                List<BiFunction<McpAsyncServerExchange, List<Root>, Mono<Void>>> asyncRootsChangeConsumers = new ArrayList<>();
                for (BiConsumer<McpSyncServerExchange, List<Root>> rootsChangeConsumer : rootsChangeConsumers) {
                    if (rootsChangeConsumer == null) {
                        continue;
                    }
                    asyncRootsChangeConsumers.add((exchange, roots) -> Mono.fromRunnable(
                            () -> rootsChangeConsumer.accept(new McpSyncServerExchange(exchange), roots))
                    );
                }
                this.asyncBuilder.rootsChangeConsumers(asyncRootsChangeConsumers);
            }
            return this;
        }

        @SafeVarargs
        public final Builder rootsChangeConsumers(BiConsumer<McpSyncServerExchange, List<Root>>...
                                                          rootsChangeConsumers) {
            return this.rootsChangeConsumers(Arrays.asList(rootsChangeConsumers));
        }

        private void beforeBuild() {
            final List<AsyncToolSpec> asyncToolSpecs = new ArrayList<>();
            for (SyncToolSpec syncToolSpec : this.toolSpecs) {
                asyncToolSpecs.add(AsyncToolSpec.fromSync(syncToolSpec, this.immediateExecution));
            }
            this.asyncBuilder.toolSpecs(asyncToolSpecs);

            final List<AsyncResourceSpec> asyncResourceSpecs = new ArrayList<>();
            for (SyncResourceSpec syncResourceSpec : this.resourceSpecs) {
                asyncResourceSpecs.add(AsyncResourceSpec.fromSync(syncResourceSpec, this.immediateExecution));
            }
            this.asyncBuilder.resourceSpecs(asyncResourceSpecs);

            final List<AsyncPromptSpec> asyncPromptSpecs = new ArrayList<>();
            for (SyncPromptSpec syncPromptSpec : this.promptSpecs) {
                asyncPromptSpecs.add(AsyncPromptSpec.fromSync(syncPromptSpec, this.immediateExecution));
            }
            this.asyncBuilder.promptSpecs(asyncPromptSpecs);

            final List<AsyncCompletionSpec> asyncCompletionSpecs = new ArrayList<>();
            for (SyncCompletionSpec syncCompletionSpec : this.completionSpecs) {
                asyncCompletionSpecs.add(AsyncCompletionSpec.fromSync(syncCompletionSpec, this.immediateExecution));
            }
            this.asyncBuilder.completionSpecs(asyncCompletionSpecs);
        }

        public McpSyncServer buildSingleSessionMcpServer(McpServerSessionTransportProvider sessionTransportProvider) {
            this.beforeBuild();
            McpAsyncServer asyncServer = this.asyncBuilder.buildSingleSessionMcpServer(sessionTransportProvider);
            return new McpSyncServer(asyncServer, this.immediateExecution);
        }
    }
}
