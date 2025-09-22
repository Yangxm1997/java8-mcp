package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransportContext;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CallToolRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CallToolResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CompleteReference;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CompleteRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CompleteResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.GetPromptRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.GetPromptResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Prompt;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ReadResourceRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ReadResourceResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Resource;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Tool;

import java.util.function.BiFunction;

@SuppressWarnings("unused")
public final class McpStatelessServerFeatures {
    private McpStatelessServerFeatures() {
    }

    public static final class AsyncToolSpec {
        private final Tool tool;
        private final BiFunction<McpTransportContext, CallToolRequest, Mono<CallToolResult>> callHandler;

        private AsyncToolSpec(Tool tool,
                              BiFunction<McpTransportContext, CallToolRequest, Mono<CallToolResult>> callHandler) {
            this.tool = tool;
            this.callHandler = callHandler;
        }

        public Tool tool() {
            return tool;
        }

        public BiFunction<McpTransportContext, CallToolRequest, Mono<CallToolResult>> callHandler() {
            return callHandler;
        }

        public AsyncToolSpec withCallHandler(BiFunction<McpTransportContext, CallToolRequest, Mono<CallToolResult>> callHandler) {
            return new AsyncToolSpec(tool, callHandler);
        }

        public static AsyncToolSpec fromSync(SyncToolSpec syncToolSpec, boolean immediateExecution) {
            if (syncToolSpec == null) {
                return null;
            }
            return new AsyncToolSpec(syncToolSpec.tool(), (ctx, req) -> {
                Mono<CallToolResult> toolResult = Mono.fromCallable(
                        () -> syncToolSpec.callHandler().apply(ctx, req)
                );
                return immediateExecution ? toolResult : toolResult.subscribeOn(Schedulers.boundedElastic());
            });
        }

        public static AsyncToolSpec.Builder builder() {
            return new AsyncToolSpec.Builder();
        }

        public static final class Builder {
            private Tool tool;
            private BiFunction<McpTransportContext, CallToolRequest, Mono<CallToolResult>> callHandler;

            private Builder() {
            }

            public AsyncToolSpec.Builder tool(Tool tool) {
                this.tool = tool;
                return this;
            }

            public AsyncToolSpec.Builder callHandler(BiFunction<McpTransportContext, CallToolRequest, Mono<CallToolResult>> callHandler) {
                this.callHandler = callHandler;
                return this;
            }

            public AsyncToolSpec build() {
                Assert.notNull(tool, "tool must not be null");
                Assert.notNull(callHandler, "callHandler must not be null");
                return new AsyncToolSpec(tool, callHandler);
            }
        }
    }

    public static final class AsyncResourceSpec {
        private final Resource resource;
        private final BiFunction<McpTransportContext, ReadResourceRequest, Mono<ReadResourceResult>> readHandler;

        private AsyncResourceSpec(Resource resource,
                                  BiFunction<McpTransportContext, ReadResourceRequest, Mono<ReadResourceResult>> readHandler) {
            this.resource = resource;
            this.readHandler = readHandler;
        }

        public Resource resource() {
            return resource;
        }

        public BiFunction<McpTransportContext, ReadResourceRequest, Mono<ReadResourceResult>> readHandler() {
            return readHandler;
        }


        public static AsyncResourceSpec fromSync(SyncResourceSpec syncResourceSpec, boolean immediateExecution) {
            if (syncResourceSpec == null) {
                return null;
            }
            return new AsyncResourceSpec(syncResourceSpec.resource(), (ctx, req) -> {
                Mono<ReadResourceResult> resourceResult = Mono.fromCallable(
                        () -> syncResourceSpec.readHandler().apply(ctx, req)
                );
                return immediateExecution ? resourceResult : resourceResult.subscribeOn(Schedulers.boundedElastic());
            });
        }

        public static AsyncResourceSpec.Builder builder() {
            return new AsyncResourceSpec.Builder();
        }

        public static final class Builder {
            private Resource resource;
            private BiFunction<McpTransportContext, ReadResourceRequest, Mono<ReadResourceResult>> readHandler;

            private Builder() {
            }

            public AsyncResourceSpec.Builder resource(Resource resource) {
                this.resource = resource;
                return this;
            }

            public AsyncResourceSpec.Builder readHandler(BiFunction<McpTransportContext, ReadResourceRequest, Mono<ReadResourceResult>> readHandler) {
                this.readHandler = readHandler;
                return this;
            }

            public AsyncResourceSpec build() {
                Assert.notNull(resource, "resource must not be null");
                Assert.notNull(readHandler, "readHandler must not be null");
                return new AsyncResourceSpec(resource, readHandler);
            }
        }
    }

    public static final class AsyncPromptSpec {
        private final Prompt prompt;
        private final BiFunction<McpTransportContext, GetPromptRequest, Mono<GetPromptResult>> promptHandler;

        private AsyncPromptSpec(Prompt prompt,
                                BiFunction<McpTransportContext, GetPromptRequest, Mono<GetPromptResult>> promptHandler) {
            this.prompt = prompt;
            this.promptHandler = promptHandler;
        }

        public Prompt prompt() {
            return prompt;
        }

        public BiFunction<McpTransportContext, GetPromptRequest, Mono<GetPromptResult>> promptHandler() {
            return promptHandler;
        }

        public static AsyncPromptSpec fromSync(SyncPromptSpec syncPromptSpec, boolean immediateExecution) {
            if (syncPromptSpec == null) {
                return null;
            }
            return new AsyncPromptSpec(syncPromptSpec.prompt(), (ctx, req) -> {
                Mono<GetPromptResult> promptResult = Mono.fromCallable(
                        () -> syncPromptSpec.promptHandler().apply(ctx, req)
                );
                return immediateExecution ? promptResult : promptResult.subscribeOn(Schedulers.boundedElastic());
            });
        }

        public static AsyncPromptSpec.Builder builder() {
            return new AsyncPromptSpec.Builder();
        }

        public static final class Builder {
            private Prompt prompt;
            private BiFunction<McpTransportContext, GetPromptRequest, Mono<GetPromptResult>> promptHandler;

            private Builder() {
            }

            public AsyncPromptSpec.Builder prompt(Prompt prompt) {
                this.prompt = prompt;
                return this;
            }

            public AsyncPromptSpec.Builder promptHandler(BiFunction<McpTransportContext, GetPromptRequest, Mono<GetPromptResult>> promptHandler) {
                this.promptHandler = promptHandler;
                return this;
            }

            public AsyncPromptSpec build() {
                Assert.notNull(prompt, "prompt must not be null");
                Assert.notNull(promptHandler, "promptHandler must not be null");
                return new AsyncPromptSpec(prompt, promptHandler);
            }
        }
    }

    public static final class AsyncCompletionSpec {
        private final CompleteReference referenceKey;
        private final BiFunction<McpTransportContext, CompleteRequest, Mono<CompleteResult>> completionHandler;

        private AsyncCompletionSpec(CompleteReference referenceKey,
                                    BiFunction<McpTransportContext, CompleteRequest, Mono<CompleteResult>> completionHandler) {
            this.referenceKey = referenceKey;
            this.completionHandler = completionHandler;
        }

        public CompleteReference referenceKey() {
            return referenceKey;
        }

        public BiFunction<McpTransportContext, CompleteRequest, Mono<CompleteResult>> completionHandler() {
            return completionHandler;
        }

        static AsyncCompletionSpec fromSync(SyncCompletionSpec syncCompletionSpec, boolean immediateExecution) {
            if (syncCompletionSpec == null) {
                return null;
            }
            return new AsyncCompletionSpec(syncCompletionSpec.referenceKey(), (ctx, request) -> {
                Mono<CompleteResult> completionResult = Mono.fromCallable(
                        () -> syncCompletionSpec.completionHandler().apply(ctx, request)
                );
                return immediateExecution ? completionResult : completionResult.subscribeOn(Schedulers.boundedElastic());
            });
        }

        public static AsyncCompletionSpec.Builder builder() {
            return new AsyncCompletionSpec.Builder();
        }

        public static final class Builder {
            private CompleteReference referenceKey;
            private BiFunction<McpTransportContext, CompleteRequest, Mono<CompleteResult>> completionHandler;

            private Builder() {
            }

            public AsyncCompletionSpec.Builder reference(CompleteReference referenceKey) {
                this.referenceKey = referenceKey;
                return this;
            }

            public AsyncCompletionSpec.Builder completionHandler(BiFunction<McpTransportContext, CompleteRequest, Mono<CompleteResult>> completionHandler) {
                this.completionHandler = completionHandler;
                return this;
            }

            public AsyncCompletionSpec build() {
                Assert.notNull(referenceKey, "referenceKey must not be null");
                Assert.notNull(completionHandler, "completionHandler must not be null");
                return new AsyncCompletionSpec(referenceKey, completionHandler);
            }
        }
    }

    public static final class SyncToolSpec {
        private final Tool tool;
        private final BiFunction<McpTransportContext, CallToolRequest, CallToolResult> callHandler;

        private SyncToolSpec(Tool tool,
                             BiFunction<McpTransportContext, CallToolRequest, CallToolResult> callHandler) {
            this.tool = tool;
            this.callHandler = callHandler;
        }

        public Tool tool() {
            return tool;
        }

        public BiFunction<McpTransportContext, CallToolRequest, CallToolResult> callHandler() {
            return callHandler;
        }

        public SyncToolSpec withCallHandler(BiFunction<McpTransportContext, CallToolRequest, CallToolResult> callHandler) {
            return new SyncToolSpec(tool, callHandler);
        }

        public static SyncToolSpec.Builder builder() {
            return new SyncToolSpec.Builder();
        }

        public static final class Builder {
            private Tool tool;
            private BiFunction<McpTransportContext, CallToolRequest, CallToolResult> callHandler;

            private Builder() {
            }

            public SyncToolSpec.Builder tool(Tool tool) {
                this.tool = tool;
                return this;
            }

            public SyncToolSpec.Builder callHandler(BiFunction<McpTransportContext, CallToolRequest, CallToolResult> callHandler) {
                this.callHandler = callHandler;
                return this;
            }

            public SyncToolSpec build() {
                Assert.notNull(tool, "tool must not be null");
                Assert.notNull(callHandler, "callHandler must not be null");
                return new SyncToolSpec(tool, callHandler);
            }
        }
    }

    public static final class SyncResourceSpec {
        private final Resource resource;
        private final BiFunction<McpTransportContext, ReadResourceRequest, ReadResourceResult> readHandler;

        private SyncResourceSpec(Resource resource,
                                 BiFunction<McpTransportContext, ReadResourceRequest, ReadResourceResult> readHandler) {
            this.resource = resource;
            this.readHandler = readHandler;
        }

        public Resource resource() {
            return resource;
        }

        public BiFunction<McpTransportContext, ReadResourceRequest, ReadResourceResult> readHandler() {
            return readHandler;
        }

        public static SyncResourceSpec.Builder builder() {
            return new SyncResourceSpec.Builder();
        }

        public static final class Builder {
            private Resource resource;
            private BiFunction<McpTransportContext, ReadResourceRequest, ReadResourceResult> readHandler;

            private Builder() {
            }

            public SyncResourceSpec.Builder resource(Resource resource) {
                this.resource = resource;
                return this;
            }

            public SyncResourceSpec.Builder readHandler(BiFunction<McpTransportContext, ReadResourceRequest, ReadResourceResult> readHandler) {
                this.readHandler = readHandler;
                return this;
            }

            public SyncResourceSpec build() {
                Assert.notNull(resource, "resource must not be null");
                Assert.notNull(readHandler, "readHandler must not be null");
                return new SyncResourceSpec(resource, readHandler);
            }
        }
    }

    public static final class SyncPromptSpec {
        private final Prompt prompt;
        private final BiFunction<McpTransportContext, GetPromptRequest, GetPromptResult> promptHandler;

        private SyncPromptSpec(Prompt prompt,
                               BiFunction<McpTransportContext, GetPromptRequest, GetPromptResult> promptHandler) {
            this.prompt = prompt;
            this.promptHandler = promptHandler;
        }

        public Prompt prompt() {
            return prompt;
        }

        public BiFunction<McpTransportContext, GetPromptRequest, GetPromptResult> promptHandler() {
            return promptHandler;
        }

        public static SyncPromptSpec.Builder builder() {
            return new SyncPromptSpec.Builder();
        }

        public static final class Builder {
            private Prompt prompt;
            private BiFunction<McpTransportContext, GetPromptRequest, GetPromptResult> promptHandler;

            private Builder() {
            }

            public SyncPromptSpec.Builder prompt(Prompt prompt) {
                this.prompt = prompt;
                return this;
            }

            public SyncPromptSpec.Builder promptHandler(BiFunction<McpTransportContext, GetPromptRequest, GetPromptResult> promptHandler) {
                this.promptHandler = promptHandler;
                return this;
            }

            public SyncPromptSpec build() {
                Assert.notNull(prompt, "prompt must not be null");
                Assert.notNull(promptHandler, "promptHandler must not be null");
                return new SyncPromptSpec(prompt, promptHandler);
            }
        }
    }

    public static final class SyncCompletionSpec {
        private final CompleteReference referenceKey;
        private final BiFunction<McpTransportContext, CompleteRequest, CompleteResult> completionHandler;

        private SyncCompletionSpec(CompleteReference referenceKey,
                                   BiFunction<McpTransportContext, CompleteRequest, CompleteResult> completionHandler) {
            this.referenceKey = referenceKey;
            this.completionHandler = completionHandler;
        }

        public CompleteReference referenceKey() {
            return referenceKey;
        }

        public BiFunction<McpTransportContext, CompleteRequest, CompleteResult> completionHandler() {
            return completionHandler;
        }

        public static SyncCompletionSpec.Builder builder() {
            return new SyncCompletionSpec.Builder();
        }

        public static final class Builder {
            private CompleteReference referenceKey;
            private BiFunction<McpTransportContext, CompleteRequest, CompleteResult> completionHandler;

            private Builder() {
            }

            public SyncCompletionSpec.Builder reference(CompleteReference referenceKey) {
                this.referenceKey = referenceKey;
                return this;
            }

            public SyncCompletionSpec.Builder completionHandler(BiFunction<McpTransportContext, CompleteRequest, CompleteResult> completionHandler) {
                this.completionHandler = completionHandler;
                return this;
            }

            public SyncCompletionSpec build() {
                Assert.notNull(referenceKey, "referenceKey must not be null");
                Assert.notNull(completionHandler, "completionHandler must not be null");
                return new SyncCompletionSpec(referenceKey, completionHandler);
            }
        }
    }
}
