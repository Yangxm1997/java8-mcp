package top.yangxm.ai.mcp.org.springaicommunity.mcp.method.complete;

import top.yangxm.ai.mcp.commons.util.Lists;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransportContext;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpUriTemplateManager;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CompleteRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CompleteResult;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

@SuppressWarnings("unused")
public final class SyncStatelessMcpCompleteMethodCallback
        extends AbstractMcpCompleteMethodCallback
        implements BiFunction<McpTransportContext, CompleteRequest, CompleteResult> {

    private SyncStatelessMcpCompleteMethodCallback(Builder builder) {
        super(builder.method, builder.bean, builder.prompt, builder.uri, builder.uriTemplateManagerFactory);
        this.validateMethod(this.method);
    }

    @Override
    public CompleteResult apply(McpTransportContext context, CompleteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        try {
            Object[] args = this.buildArgs(this.method, context, request);
            this.method.setAccessible(true);
            Object result = this.method.invoke(this.bean, args);
            return convertToCompleteResult(result);
        } catch (Exception e) {
            throw new McpCompleteMethodException("Error invoking complete method: " + this.method.getName(), e);
        }
    }

    private CompleteResult convertToCompleteResult(Object result) {
        if (result == null) {
            return new CompleteResult(new CompleteResult.CompleteCompletion(Lists.of(), 0, false));
        }

        if (result instanceof CompleteResult) {
            return (CompleteResult) result;
        }

        if (result instanceof CompleteResult.CompleteCompletion) {
            return new CompleteResult((CompleteResult.CompleteCompletion) result);
        }

        if (result instanceof List) {
            List<?> list = (List<?>) result;
            List<String> values = new ArrayList<>();

            for (Object item : list) {
                if (item instanceof String) {
                    values.add((String) item);
                } else {
                    throw new IllegalArgumentException("List items must be of type String");
                }
            }

            return new CompleteResult(new CompleteResult.CompleteCompletion(values, values.size(), false));
        }

        if (result instanceof String) {
            return new CompleteResult(new CompleteResult.CompleteCompletion(Lists.of((String) result), 1, false));
        }

        throw new IllegalArgumentException("Unsupported return type: " + result.getClass().getName());
    }

    @Override
    protected void validateReturnType(Method method) {
        Class<?> returnType = method.getReturnType();

        boolean validReturnType = CompleteResult.class.isAssignableFrom(returnType)
                || CompleteResult.CompleteCompletion.class.isAssignableFrom(returnType)
                || List.class.isAssignableFrom(returnType)
                || String.class.isAssignableFrom(returnType);

        if (!validReturnType) {
            throw new IllegalArgumentException(
                    String.format("Method must return either CompleteResult, CompleteCompletion, List<String>, " +
                                    "or String: %s in %s returns %s",
                            method.getName(), method.getDeclaringClass().getName(), returnType.getName())
            );
        }
    }

    @Override
    protected boolean isExchangeType(Class<?> paramType) {
        return McpTransportContext.class.isAssignableFrom(paramType);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractBuilder<Builder, SyncStatelessMcpCompleteMethodCallback> {
        public Builder() {
            this.uriTemplateManagerFactory = McpUriTemplateManager.DEFAULT_FACTORY;
        }

        @Override
        public SyncStatelessMcpCompleteMethodCallback build() {
            validate();
            return new SyncStatelessMcpCompleteMethodCallback(this);
        }
    }
}
