package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.json.JsonMapper;
import top.yangxm.ai.mcp.commons.json.schema.JsonSchemaValidator;
import top.yangxm.ai.mcp.commons.logger.Logger;
import top.yangxm.ai.mcp.commons.logger.LoggerFactoryHolder;
import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.commons.util.Maps;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpUriTemplateManager;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.exception.McpError;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CallToolRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CallToolResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CompleteReference;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CompleteRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CompleteRequest.CompleteArgument;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CompleteResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ErrorCodes;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.GetPromptRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.GetPromptResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Implementation;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.InitializeRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.InitializeResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ListPromptsResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ListResourceTemplatesResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ListResourcesResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ListToolsResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Prompt;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.PromptReference;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ReadResourceRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ReadResourceResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Resource;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ResourceReference;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ResourceTemplate;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ServerCapabilities;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures.AsyncCompletionSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures.AsyncPromptSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures.AsyncResourceSpec;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpStatelessServerFeatures.AsyncToolSpec;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class McpStatelessAsyncServer {
    private static final Logger logger = LoggerFactoryHolder.getLogger(McpStatelessAsyncServer.class);

    private final McpStatelessServerTransport transportProvider;
    private final JsonMapper jsonMapper;
    private final McpUriTemplateManager.Factory uriTemplateManagerFactory;
    private final JsonSchemaValidator jsonSchemaValidator;
    private final ServerCapabilities serverCapabilities;
    private final Implementation serverInfo;
    private final String instructions;
    private final ConcurrentHashMap<String, AsyncToolSpec> toolSpecs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AsyncResourceSpec> resourceSpecs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ResourceTemplate> resourceTemplates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AsyncPromptSpec> promptSpecs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<CompleteReference, AsyncCompletionSpec> completionSpecs = new ConcurrentHashMap<>();
    private List<String> protocolVersions;

    private McpStatelessAsyncServer(McpStatelessServerTransport transportProvider,
                                    JsonMapper jsonMapper,
                                    ServerCapabilities serverCapabilities,
                                    Implementation serverInfo,
                                    String instructions,
                                    Map<String, AsyncToolSpec> toolSpecs,
                                    Map<String, AsyncResourceSpec> resourceSpecs,
                                    Map<String, ResourceTemplate> resourceTemplates,
                                    Map<String, AsyncPromptSpec> promptSpecs,
                                    Map<CompleteReference, AsyncCompletionSpec> completionSpecs,
                                    Duration requestTimeout,
                                    McpUriTemplateManager.Factory uriTemplateManagerFactory,
                                    JsonSchemaValidator jsonSchemaValidator) {
        Assert.notNull(transportProvider, "transportProvider must not be null");
        Assert.notNull(jsonMapper, "jsonMapper must not be null");
        Assert.notNull(serverCapabilities, "serverCapabilities must not be null");
        Assert.notNull(serverInfo, "serverInfo must not be null");
        Assert.notNull(instructions, "instructions must not be null");
        Assert.notNull(toolSpecs, "toolSpecs must not be null");
        Assert.notNull(resourceSpecs, "resourceSpecs must not be null");
        Assert.notNull(resourceTemplates, "resourceTemplates must not be null");
        Assert.notNull(promptSpecs, "promptSpecs must not be null");
        Assert.notNull(completionSpecs, "completionSpecs must not be null");
        Assert.notNull(requestTimeout, "requestTimeout must not be null");
        Assert.notNull(jsonSchemaValidator, "jsonSchemaValidator must not be null");

        this.transportProvider = transportProvider;
        this.jsonMapper = jsonMapper;
        this.serverInfo = serverInfo;
        this.serverCapabilities = serverCapabilities;
        this.instructions = instructions;
        toolSpecs.forEach((key, val) -> this.toolSpecs.put(key, val.withCallHandler(
                StructuredOutputCallToolHandler.withStructuredOutputHandling(
                        jsonSchemaValidator,
                        val.tool().outputSchema(),
                        val.callHandler()
                )
        )));
        this.resourceSpecs.putAll(resourceSpecs);
        this.resourceTemplates.putAll(resourceTemplates);
        this.promptSpecs.putAll(promptSpecs);
        this.completionSpecs.putAll(completionSpecs);
        this.uriTemplateManagerFactory = (uriTemplateManagerFactory != null) ? uriTemplateManagerFactory : McpUriTemplateManager.DEFAULT_FACTORY;
        this.jsonSchemaValidator = jsonSchemaValidator;

        Map<String, McpStatelessRequestHandler<?>> requestHandlers = prepareRequestHandlers();
        this.protocolVersions = new ArrayList<>(transportProvider.protocolVersions());
        McpStatelessServerHandler handler = new DefaultMcpStatelessServerHandler(requestHandlers, Maps.of());
        transportProvider.setMcpHandler(handler);
    }

    private Map<String, McpStatelessRequestHandler<?>> prepareRequestHandlers() {
        Map<String, McpStatelessRequestHandler<?>> requestHandlers = new HashMap<>();

        requestHandlers.put(McpSchema.METHOD_PING, (ctx, params) -> Mono.just(Maps.of()));
        requestHandlers.put(McpSchema.METHOD_INITIALIZE, asyncInitializeRequestHandler());

        if (this.serverCapabilities.tools() != null) {
            requestHandlers.put(McpSchema.METHOD_TOOLS_LIST, toolsListRequestHandler());
            requestHandlers.put(McpSchema.METHOD_TOOLS_CALL, toolsCallRequestHandler());
        }

        if (this.serverCapabilities.resources() != null) {
            requestHandlers.put(McpSchema.METHOD_RESOURCES_LIST, resourcesListRequestHandler());
            requestHandlers.put(McpSchema.METHOD_RESOURCES_READ, resourcesReadRequestHandler());
            requestHandlers.put(McpSchema.METHOD_RESOURCES_TEMPLATES_LIST, resourceTemplateListRequestHandler());
        }

        if (this.serverCapabilities.prompts() != null) {
            requestHandlers.put(McpSchema.METHOD_PROMPT_LIST, promptsListRequestHandler());
            requestHandlers.put(McpSchema.METHOD_PROMPT_GET, promptsGetRequestHandler());
        }


        if (this.serverCapabilities.completions() != null) {
            requestHandlers.put(McpSchema.METHOD_COMPLETION_COMPLETE, completionCompleteRequestHandler());
        }

        return requestHandlers;
    }

    private McpStatelessRequestHandler<InitializeResult> asyncInitializeRequestHandler() {
        return (ctx, req) -> Mono.defer(() -> {
            InitializeRequest initializeRequest = this.jsonMapper.convertValue(req, InitializeRequest.class);
            logger.info("Client initialize request - Protocol: {}, Capabilities: {}, Info: {}",
                    initializeRequest.protocolVersion(), initializeRequest.capabilities(),
                    initializeRequest.clientInfo());

            String serverProtocolVersion = this.protocolVersions.get(this.protocolVersions.size() - 1);

            if (this.protocolVersions.contains(initializeRequest.protocolVersion())) {
                serverProtocolVersion = initializeRequest.protocolVersion();
            } else {
                logger.warn("Client requested unsupported protocol version: {}, so the server will suggest the {} version instead",
                        initializeRequest.protocolVersion(), serverProtocolVersion);
            }
            return Mono.just(new InitializeResult(serverProtocolVersion, this.serverCapabilities, this.serverInfo, this.instructions));
        });
    }

    public ServerCapabilities serverCapabilities() {
        return this.serverCapabilities;
    }

    public Implementation serverInfo() {
        return this.serverInfo;
    }

    void setProtocolVersions(List<String> protocolVersions) {
        this.protocolVersions = protocolVersions;
    }

    public Mono<Void> addTool(AsyncToolSpec toolSpec) {
        if (toolSpec == null) {
            return Mono.error(McpError.of("Tool specification must not be null"));
        }
        if (toolSpec.tool() == null) {
            return Mono.error(McpError.of("Tool must not be null"));
        }
        if (toolSpec.callHandler() == null) {
            return Mono.error(McpError.of("Tool call handler must not be null"));
        }
        if (this.serverCapabilities.tools() == null) {
            return Mono.error(McpError.of("Server must be configured with tool capabilities"));
        }

        AsyncToolSpec wrappedToolSpec = toolSpec.withCallHandler(
                StructuredOutputCallToolHandler.withStructuredOutputHandling(
                        this.jsonSchemaValidator,
                        toolSpec.tool().outputSchema(),
                        toolSpec.callHandler()
                ));
        return Mono.defer(() -> {
            if (this.toolSpecs.putIfAbsent(wrappedToolSpec.tool().name(), wrappedToolSpec) != null) {
                return Mono.error(McpError.of("Tool with name '" + wrappedToolSpec.tool().name() + "' already exists"));
            }
            logger.debug("Added tool handler: {}", wrappedToolSpec.tool().name());
            return Mono.empty();
        });
    }

    public Mono<Void> removeTool(String toolName) {
        if (toolName == null) {
            return Mono.error(McpError.of("Tool name must not be null"));
        }
        if (this.serverCapabilities.tools() == null) {
            return Mono.error(McpError.of("Server must be configured with tool capabilities"));
        }

        return Mono.defer(() -> {
            AsyncToolSpec removed = this.toolSpecs.remove(toolName);
            if (removed != null) {
                logger.debug("Removed tool handler: {}", toolName);
                return Mono.empty();
            }
            return Mono.error(McpError.of("Tool with name '" + toolName + "' not found"));
        });
    }

    private List<McpSchema.Tool> getToolList() {
        return this.toolSpecs.values().stream().map(AsyncToolSpec::tool).collect(Collectors.toList());
    }

    private Optional<AsyncToolSpec> getToolSpec(String toolName) {
        AsyncToolSpec specification = this.toolSpecs.get(toolName);
        if (specification == null) {
            return Optional.empty();
        }
        return Optional.of(specification);
    }

    private McpStatelessRequestHandler<ListToolsResult> toolsListRequestHandler() {
        return (ctx, params) -> Mono.just(new ListToolsResult(this.getToolList(), null));
    }

    private McpStatelessRequestHandler<CallToolResult> toolsCallRequestHandler() {
        return (ctx, params) -> {
            CallToolRequest callToolRequest = jsonMapper.convertValue(params, CallToolRequest.class);
            String name = callToolRequest.name();
            return this.getToolSpec(name)
                    .map(ts -> Mono.defer(() -> ts.callHandler().apply(ctx, callToolRequest)))
                    .orElseThrow(() -> McpError.of(ErrorCodes.INVALID_PARAMS,
                            "Unknown tool: invalid_tool_name",
                            "Tool not found: " + name)
                    );
        };
    }

    public Mono<Void> addResource(AsyncResourceSpec resourceSpec) {
        if (resourceSpec == null) {
            return Mono.error(McpError.of("Resource specification must not be null"));
        }

        if (resourceSpec.resource() == null) {
            return Mono.error(McpError.of("Resource must not be null"));
        }

        if (resourceSpec.readHandler() == null) {
            return Mono.error(McpError.of("Resource read handler must not be null"));
        }

        if (this.serverCapabilities.resources() == null) {
            return Mono.error(McpError.of("Server must be configured with resource capabilities"));
        }

        return Mono.defer(() -> {
            if (this.resourceSpecs.putIfAbsent(resourceSpec.resource().uri(), resourceSpec) != null) {
                return Mono.error(McpError.of("Resource with URI '" + resourceSpec.resource().uri() + "' already exists"));
            }
            logger.debug("Added resource handler: {}", resourceSpec.resource().uri());
            return Mono.empty();
        });
    }

    public Mono<Void> removeResource(String resourceUri) {
        if (resourceUri == null) {
            return Mono.error(McpError.of("Resource URI must not be null"));
        }

        if (this.serverCapabilities.resources() == null) {
            return Mono.error(McpError.of("Server must be configured with resource capabilities"));
        }

        return Mono.defer(() -> {
            AsyncResourceSpec removed = this.resourceSpecs.remove(resourceUri);
            if (removed != null) {
                logger.debug("Removed resource handler: {}", resourceUri);
                return Mono.empty();
            }
            return Mono.error(McpError.of("Resource with URI '" + resourceUri + "' not found"));
        });
    }

    private List<Resource> getResourceList() {
        return this.resourceSpecs.values()
                .stream()
                .map(AsyncResourceSpec::resource)
                .filter(resource -> !resource.uri().contains("{"))
                .collect(Collectors.toList());
    }

    private List<ResourceTemplate> getResourceTemplateList() {
        List<ResourceTemplate> list = new ArrayList<>(this.resourceTemplates.values());
        List<ResourceTemplate> resourceTemplates = this.resourceSpecs.keySet()
                .stream()
                .filter(uri -> uri.contains("{"))
                .map(uri -> {
                    Resource resource = this.resourceSpecs.get(uri).resource();
                    return new ResourceTemplate(resource.uri(), resource.name(), resource.title(),
                            resource.description(), resource.mimeType(), resource.annotations());
                })
                .collect(Collectors.toList());

        list.addAll(resourceTemplates);
        return list;
    }

    private Optional<AsyncResourceSpec> getResourceSpec(String resourceUri) {
        return this.resourceSpecs.values()
                .stream()
                .filter(specification -> this.uriTemplateManagerFactory
                        .create(specification.resource().uri())
                        .matches(resourceUri))
                .findFirst();
    }

    private McpStatelessRequestHandler<ListResourcesResult> resourcesListRequestHandler() {
        return (ctx, params) -> Mono.just(new ListResourcesResult(this.getResourceList(), null));
    }

    private McpStatelessRequestHandler<ListResourceTemplatesResult> resourceTemplateListRequestHandler() {
        return (ctx, params) -> Mono.just(
                new ListResourceTemplatesResult(this.getResourceTemplateList(), null));
    }

    private McpStatelessRequestHandler<ReadResourceResult> resourcesReadRequestHandler() {
        return (ctx, params) -> {
            ReadResourceRequest resourceRequest = jsonMapper.convertValue(params, ReadResourceRequest.class);
            String resourceUri = resourceRequest.uri();
            return this.getResourceSpec(resourceUri)
                    .map(rs -> Mono.defer(() -> rs.readHandler().apply(ctx, resourceRequest)))
                    .orElseThrow(() -> McpError.of(ErrorCodes.INVALID_PARAMS,
                            "Unknown resource: invalid_resource_uri",
                            "Resource not found: " + resourceUri)
                    );
        };
    }

    public Mono<Void> addPrompt(AsyncPromptSpec promptSpec) {
        if (promptSpec == null) {
            return Mono.error(McpError.of("Prompt specification must not be null"));
        }

        if (promptSpec.prompt() == null) {
            return Mono.error(McpError.of("Prompt must not be null"));
        }

        if (promptSpec.promptHandler() == null) {
            return Mono.error(McpError.of("Prompt handler must not be null"));
        }

        if (this.serverCapabilities.prompts() == null) {
            return Mono.error(McpError.of("Server must be configured with prompt capabilities"));
        }

        return Mono.defer(() -> {
            if (this.promptSpecs.putIfAbsent(promptSpec.prompt().name(), promptSpec) != null) {
                return Mono.error(McpError.of("Prompt with name '" + promptSpec.prompt().name() + "' already exists"));
            }
            logger.debug("Added prompt handler: {}", promptSpec.prompt().name());
            return Mono.empty();
        });
    }

    public Mono<Void> removePrompt(String promptName) {
        if (promptName == null) {
            return Mono.error(McpError.of("Prompt name must not be null"));
        }
        if (this.serverCapabilities.prompts() == null) {
            return Mono.error(McpError.of("Server must be configured with prompt capabilities"));
        }

        return Mono.defer(() -> {
            AsyncPromptSpec removed = this.promptSpecs.remove(promptName);
            if (removed != null) {
                logger.debug("Removed prompt handler: {}", promptName);
                return Mono.empty();
            }
            return Mono.error(McpError.of("Prompt with name '" + promptName + "' not found"));
        });
    }

    private List<Prompt> getPromptList() {
        return this.promptSpecs.values()
                .stream()
                .map(AsyncPromptSpec::prompt)
                .collect(Collectors.toList());
    }

    private Optional<AsyncPromptSpec> getPromptSpec(String promptName) {
        AsyncPromptSpec specification = this.promptSpecs.get(promptName);
        if (specification == null) {
            return Optional.empty();
        }
        return Optional.of(specification);
    }

    private McpStatelessRequestHandler<ListPromptsResult> promptsListRequestHandler() {
        return (ctx, params) -> {
            // TODO: Implement pagination
            return Mono.just(new ListPromptsResult(this.getPromptList(), null));
        };
    }

    private McpStatelessRequestHandler<GetPromptResult> promptsGetRequestHandler() {
        return (ctx, params) -> {
            GetPromptRequest promptRequest = jsonMapper.convertValue(params, GetPromptRequest.class);
            String name = promptRequest.name();
            return this.getPromptSpec(name)
                    .map(ps -> Mono.defer(() -> ps.promptHandler().apply(ctx, promptRequest)))
                    .orElseThrow(() -> McpError.of(ErrorCodes.INVALID_PARAMS,
                            "Unknown prompt: invalid_prompt_name",
                            "Prompt not found: " + name)
                    );
        };
    }

    private McpStatelessRequestHandler<CompleteResult> completionCompleteRequestHandler() {
        return (ctx, params) -> {
            CompleteRequest request = parseCompletionParams(params);

            if (request.ref() == null) {
                return Mono.error(McpError.of("ref must not be null"));
            }

            if (request.ref().type() == null) {
                return Mono.error(McpError.of("type must not be null"));
            }

            String type = request.ref().type();
            String argumentName = request.argument().name();

            if (type.equals(CompleteReference.REF_PROMPT) && (request.ref() instanceof PromptReference)) {
                PromptReference promptReference = (PromptReference) request.ref();
                AsyncPromptSpec promptSpec = this.promptSpecs.get(promptReference.name());
                if (promptSpec == null) {
                    return Mono.error(McpError.of("Prompt not found: " + promptReference.name()));
                }
                if (promptSpec.prompt()
                        .arguments()
                        .stream()
                        .noneMatch(arg -> arg.name().equals(argumentName))) {
                    return Mono.error(McpError.of("Argument not found: " + argumentName));
                }
            }

            if (type.equals(CompleteReference.REF_RESOURCE) && request.ref() instanceof ResourceReference) {
                ResourceReference resourceReference = (ResourceReference) request.ref();
                AsyncResourceSpec resourceSpec = this.resourceSpecs.get(resourceReference.uri());
                if (resourceSpec == null) {
                    return Mono.error(McpError.of("Resource not found: " + resourceReference.uri()));
                }
                if (!uriTemplateManagerFactory.create(resourceSpec.resource().uri())
                        .getVariableNames()
                        .contains(argumentName)) {
                    return Mono.error(McpError.of("Argument not found: " + argumentName));
                }
            }

            AsyncCompletionSpec specification = this.completionSpecs.get(request.ref());
            if (specification == null) {
                return Mono.error(McpError.of("AsyncCompletionSpec not found: " + request.ref()));
            }
            return specification.completionHandler().apply(ctx, request);
        };
    }

    @SuppressWarnings("unchecked")
    private CompleteRequest parseCompletionParams(Object object) {
        Map<String, Object> params = (Map<String, Object>) object;
        Map<String, Object> refMap = (Map<String, Object>) params.get("ref");
        Map<String, Object> argMap = (Map<String, Object>) params.get("argument");

        String refType = (String) refMap.get("type");

        CompleteReference ref;
        switch (refType) {
            case CompleteReference.REF_PROMPT:
                ref = new PromptReference(refType, (String) refMap.get("name"), refMap.get("title") != null ? (String) refMap.get("title") : null);
                break;
            case CompleteReference.REF_RESOURCE:
                ref = new McpSchema.ResourceReference(refType, (String) refMap.get("uri"));
                break;
            default:
                throw new IllegalArgumentException("Invalid ref type: " + refType);
        }

        String argName = (String) argMap.get("name");
        String argValue = (String) argMap.get("value");
        CompleteArgument argument = new CompleteArgument(argName, argValue);
        return new CompleteRequest(ref, argument);
    }

    public Mono<Void> closeGracefully() {
        return this.transportProvider.closeGracefully();
    }

    public void close() {
        this.transportProvider.close();
    }

    public static Builder builder() {
        return new Builder();
    }

    public final static class Builder {
        private McpServerTransportProviderBase transportProvider;
        private JsonMapper jsonMapper = JsonMapper.getDefault();
        private McpUriTemplateManager.Factory uriTemplateManagerFactory = McpUriTemplateManager.DEFAULT_FACTORY;
        private JsonSchemaValidator jsonSchemaValidator = JsonSchemaValidator.getDefault();
        private ServerCapabilities serverCapabilities = null;
        private Implementation serverInfo = McpServerConst.DEFAULT_SERVER_INFO;
        private String instructions = "";
        private Duration requestTimeout = McpServerConst.DEFAULT_REQUEST_TIMEOUT;
        private final Map<String, AsyncToolSpec> toolSpecs = new HashMap<>();
        private final Map<String, AsyncResourceSpec> resourceSpecs = new HashMap<>();
        private final Map<String, ResourceTemplate> resourceTemplates = new HashMap<>();
        private final Map<String, AsyncPromptSpec> promptSpecs = new HashMap<>();
        private final Map<CompleteReference, AsyncCompletionSpec> completionSpecs = new HashMap<>();

        private Builder() {
        }

        public Builder jsonMapper(JsonMapper jsonMapper) {
            Assert.notNull(jsonMapper, "jsonMapper must not be null");
            this.jsonMapper = jsonMapper;
            return this;
        }

        public Builder uriTemplateManagerFactory(McpUriTemplateManager.Factory uriTemplateManagerFactory) {
            Assert.notNull(uriTemplateManagerFactory, "uriTemplateManagerFactory must not be null");
            this.uriTemplateManagerFactory = uriTemplateManagerFactory;
            return this;
        }

        public Builder jsonSchemaValidator(JsonSchemaValidator jsonSchemaValidator) {
            Assert.notNull(jsonSchemaValidator, "jsonSchemaValidator must not be null");
            this.jsonSchemaValidator = jsonSchemaValidator;
            return this;
        }

        public Builder serverCapabilities(ServerCapabilities serverCapabilities) {
            Assert.notNull(serverCapabilities, "serverCapabilities must not be null");
            this.serverCapabilities = serverCapabilities;
            return this;
        }

        public Builder serverInfo(Implementation serverInfo) {
            Assert.notNull(serverInfo, "serverInfo must not be null");
            this.serverInfo = serverInfo;
            return this;
        }

        public Builder serverInfo(String name, String version) {
            Assert.hasText(name, "Name must not be null or empty");
            Assert.hasText(version, "Version must not be null or empty");
            this.serverInfo = new Implementation(name, version);
            return this;
        }

        public Builder instructions(String instructions) {
            if (instructions != null) {
                this.instructions = instructions;
            }
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            Assert.notNull(requestTimeout, "requestTimeout must not be null");
            this.requestTimeout = requestTimeout;
            return this;
        }

        public Builder toolSpecs(List<AsyncToolSpec> toolSpecs) {
            if (toolSpecs != null) {
                for (AsyncToolSpec toolSpec : toolSpecs) {
                    if (toolSpec == null) {
                        continue;
                    }
                    final String toolName = toolSpec.tool().name();
                    if (this.toolSpecs.putIfAbsent(toolName, toolSpec) != null) {
                        throw new IllegalStateException("Duplicate tool name: " + toolName);
                    }
                }
            }
            return this;
        }

        public Builder toolSpecs(AsyncToolSpec... toolSpecs) {
            return this.toolSpecs(Arrays.asList(toolSpecs));
        }

        public Builder resourceSpecs(List<AsyncResourceSpec> resourceSpecs) {
            if (resourceSpecs != null) {
                for (AsyncResourceSpec resourceSpec : resourceSpecs) {
                    if (resourceSpec == null) {
                        continue;
                    }
                    final String uri = resourceSpec.resource().uri();
                    if (this.resourceSpecs.putIfAbsent(uri, resourceSpec) != null) {
                        throw new IllegalStateException("Duplicate resource uri: " + uri);
                    }
                }
            }
            return this;
        }

        public Builder resourceSpecs(AsyncResourceSpec... resourceSpecs) {
            return this.resourceSpecs(Arrays.asList(resourceSpecs));
        }

        public Builder resourceTemplates(List<ResourceTemplate> resourceTemplates) {
            if (resourceTemplates != null) {
                for (ResourceTemplate resourceTemplate : resourceTemplates) {
                    if (resourceTemplate == null) {
                        continue;
                    }
                    final String uri = resourceTemplate.getUriTemplate();
                    if (this.resourceTemplates.putIfAbsent(uri, resourceTemplate) != null) {
                        throw new IllegalStateException("Duplicate resource template uri: " + uri);
                    }
                }
            }
            return this;
        }

        public Builder resourceTemplates(ResourceTemplate... resourceTemplates) {
            return this.resourceTemplates(Arrays.asList(resourceTemplates));
        }

        public Builder promptSpecs(List<AsyncPromptSpec> promptSpecs) {
            if (promptSpecs != null) {
                for (AsyncPromptSpec promptSpec : promptSpecs) {
                    if (promptSpec == null) {
                        continue;
                    }
                    final String promptName = promptSpec.prompt().name();
                    if (this.promptSpecs.putIfAbsent(promptName, promptSpec) != null) {
                        throw new IllegalStateException("Duplicate prompt name: " + promptName);
                    }
                }
            }
            return this;
        }

        public Builder promptSpecs(AsyncPromptSpec... promptSpecs) {
            return this.promptSpecs(Arrays.asList(promptSpecs));
        }

        public Builder completionSpecs(List<AsyncCompletionSpec> completionSpecs) {
            if (completionSpecs != null) {
                for (AsyncCompletionSpec completionSpec : completionSpecs) {
                    if (completionSpec == null) {
                        continue;
                    }
                    if (this.completionSpecs.putIfAbsent(completionSpec.referenceKey(), completionSpec) != null) {
                        throw new IllegalStateException("Duplicate completion reference key: " + completionSpec.referenceKey());
                    }
                }
            }
            return this;
        }

        public Builder completionSpecs(AsyncCompletionSpec... completionSpecs) {
            return this.completionSpecs(Arrays.asList(completionSpecs));
        }


        public McpStatelessAsyncServer build(McpStatelessServerTransport transportProvider) {
            Assert.notNull(transportProvider, "transportProvider must not be null");
            if (this.serverCapabilities == null) {
                this.serverCapabilities = new ServerCapabilities(
                        null, // completions
                        null, // experimental
                        null, // logging
                        !Maps.isEmpty(promptSpecs) ? new ServerCapabilities.PromptCapabilities(false) : null,
                        !Maps.isEmpty(resourceSpecs) ? new ServerCapabilities.ResourceCapabilities(false, false) : null,
                        !Maps.isEmpty(toolSpecs) ? new ServerCapabilities.ToolCapabilities(false) : null);
            }
            return new McpStatelessAsyncServer(transportProvider, jsonMapper,
                    this.serverCapabilities, this.serverInfo, this.instructions,
                    this.toolSpecs, this.resourceSpecs, this.resourceTemplates, this.promptSpecs, this.completionSpecs,
                    this.requestTimeout, this.uriTemplateManagerFactory, this.jsonSchemaValidator);
        }
    }
}
