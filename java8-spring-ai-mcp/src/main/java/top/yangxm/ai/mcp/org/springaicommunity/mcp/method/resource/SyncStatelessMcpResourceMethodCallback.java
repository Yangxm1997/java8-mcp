package top.yangxm.ai.mcp.org.springaicommunity.mcp.method.resource;

import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransportContext;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ReadResourceRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ReadResourceResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ResourceContents;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@SuppressWarnings("unused")
public final class SyncStatelessMcpResourceMethodCallback
        extends AbstractMcpResourceMethodCallback
        implements BiFunction<McpTransportContext, ReadResourceRequest, ReadResourceResult> {

    private SyncStatelessMcpResourceMethodCallback(Builder builder) {
        super(builder.method, builder.bean, builder.uri, builder.name, builder.description, builder.mimeType,
                builder.resultConverter, builder.uriTemplateManagerFactory, builder.contentType);
        this.validateMethod(this.method);
    }

    @Override
    public ReadResourceResult apply(McpTransportContext context, ReadResourceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }
        try {
            Map<String, String> uriVariableValues = this.uriTemplateManager.extractVariableValues(request.uri());
            if (!this.uriVariables.isEmpty() && uriVariableValues.size() != this.uriVariables.size()) {
                throw new IllegalArgumentException(
                        String.format("Failed to extract all URI variables from request URI: %s. " +
                                        "Expected variables: %s, but found: %s",
                                request.uri(), this.uriVariables, uriVariableValues.keySet())
                );
            }

            Object[] args = this.buildArgs(this.method, context, request, uriVariableValues);
            this.method.setAccessible(true);
            Object result = this.method.invoke(this.bean, args);

            return this.resultConverter.convertToReadResourceResult(
                    result, request.uri(), this.mimeType, this.contentType);
        } catch (Exception e) {
            throw new McpResourceMethodException("Access error invoking resource method: " + this.method.getName(), e);
        }
    }

    public static class Builder extends AbstractBuilder<Builder, SyncStatelessMcpResourceMethodCallback> {
        private Builder() {
            this.resultConverter = new DefaultMcpReadResourceResultConverter();
        }

        @Override
        public SyncStatelessMcpResourceMethodCallback build() {
            validate();
            return new SyncStatelessMcpResourceMethodCallback(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected void validateReturnType(Method method) {
        Class<?> returnType = method.getReturnType();

        boolean validReturnType = ReadResourceResult.class.isAssignableFrom(returnType)
                || List.class.isAssignableFrom(returnType)
                || ResourceContents.class.isAssignableFrom(returnType)
                || String.class.isAssignableFrom(returnType);

        if (!validReturnType) {
            throw new IllegalArgumentException(
                    String.format("Method must return either ReadResourceResult, List<ResourceContents>, List<String>, " +
                                    "ResourceContents, or String: %s in %s returns %s",
                            method.getName(), method.getDeclaringClass().getName(), returnType.getName())
            );
        }
    }

    @Override
    protected boolean isExchangeOrContextType(Class<?> paramType) {
        return McpTransportContext.class.isAssignableFrom(paramType);
    }
}
