package top.yangxm.ai.mcp.org.springaicommunity.mcp.method.resource;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransportContext;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ReadResourceRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ReadResourceResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ResourceContents;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpAsyncServerExchange;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@SuppressWarnings("unused")
public final class AsyncStatelessMcpResourceMethodCallback
        extends AbstractMcpResourceMethodCallback
        implements BiFunction<McpTransportContext, ReadResourceRequest, Mono<ReadResourceResult>> {

    private AsyncStatelessMcpResourceMethodCallback(Builder builder) {
        super(builder.method, builder.bean, builder.uri, builder.name, builder.description, builder.mimeType,
                builder.resultConverter, builder.uriTemplateManagerFactory, builder.contentType);
        this.validateMethod(this.method);
    }

    @Override
    public Mono<ReadResourceResult> apply(McpTransportContext context, ReadResourceRequest request) {
        if (request == null) {
            return Mono.error(new IllegalArgumentException("Request must not be null"));
        }

        return Mono.defer(() -> {
            try {
                Map<String, String> uriVariableValues = this.uriTemplateManager.extractVariableValues(request.uri());
                if (!this.uriVariables.isEmpty() && uriVariableValues.size() != this.uriVariables.size()) {
                    return Mono.error(new IllegalArgumentException(
                            String.format("Failed to extract all URI variables from request URI: %s." +
                                            "Expected variables: %s, but found: %s",
                                    request.uri(), this.uriVariables, uriVariableValues.keySet()))
                    );
                }

                Object[] args = this.buildArgs(this.method, context, request, uriVariableValues);
                this.method.setAccessible(true);
                Object result = this.method.invoke(this.bean, args);

                if (result instanceof Mono<?>) {
                    return ((Mono<?>) result).map(r -> this.resultConverter.convertToReadResourceResult(r,
                            request.uri(), this.mimeType, this.contentType));
                } else {
                    return Mono.just(this.resultConverter.convertToReadResourceResult(result, request.uri(),
                            this.mimeType, this.contentType));
                }
            } catch (Exception e) {
                return Mono.error(new McpResourceMethodException("Error invoking resource method: " + this.method.getName(), e));
            }
        });
    }

    @Override
    protected void validateReturnType(Method method) {
        Class<?> returnType = method.getReturnType();

        boolean validReturnType = ReadResourceResult.class.isAssignableFrom(returnType)
                || List.class.isAssignableFrom(returnType)
                || ResourceContents.class.isAssignableFrom(returnType)
                || String.class.isAssignableFrom(returnType)
                || Mono.class.isAssignableFrom(returnType);

        if (!validReturnType) {
            throw new IllegalArgumentException(
                    String.format("Method must return either ReadResourceResult, List<ResourceContents>, List<String>, " +
                                    "ResourceContents, String, or Mono<T>: %s in %s returns %s",
                            method.getName(), method.getDeclaringClass().getName(), returnType.getName())
            );
        }
    }

    @Override
    protected boolean isExchangeOrContextType(Class<?> paramType) {
        return McpAsyncServerExchange.class.isAssignableFrom(paramType);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractBuilder<Builder, AsyncStatelessMcpResourceMethodCallback> {
        public Builder() {
            this.resultConverter = new DefaultMcpReadResourceResultConverter();
        }

        @Override
        public AsyncStatelessMcpResourceMethodCallback build() {
            validate();
            return new AsyncStatelessMcpResourceMethodCallback(this);
        }
    }
}
