package top.yangxm.ai.mcp.org.springaicommunity.mcp.method.complete;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.util.Lists;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpUriTemplateManager;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CompleteRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CompleteResult;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.McpAsyncServerExchange;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

@SuppressWarnings("unused")
public final class AsyncMcpCompleteMethodCallback
        extends AbstractMcpCompleteMethodCallback
        implements BiFunction<McpAsyncServerExchange, CompleteRequest, Mono<CompleteResult>> {

    private AsyncMcpCompleteMethodCallback(Builder builder) {
        super(builder.method, builder.bean, builder.prompt, builder.uri, builder.uriTemplateManagerFactory);
        this.validateMethod(this.method);
    }

    @Override
    public Mono<CompleteResult> apply(McpAsyncServerExchange exchange, CompleteRequest request) {
        if (request == null) {
            return Mono.error(new IllegalArgumentException("Request must not be null"));
        }

        try {
            Object[] args = this.buildArgs(this.method, exchange, request);
            this.method.setAccessible(true);
            Object result = this.method.invoke(this.bean, args);
            return convertToCompleteResultMono(result);
        } catch (Exception e) {
            return Mono.error(new McpCompleteMethodException("Error invoking complete method: " + this.method.getName(), e));
        }
    }

    private Mono<CompleteResult> convertToCompleteResultMono(Object result) {
        if (result == null) {
            return Mono.just(new CompleteResult(new CompleteResult.CompleteCompletion(Lists.of(), 0, false)));
        }

        if (result instanceof Mono) {
            return ((Mono<?>) result).map(this::convertToCompleteResult);
        }

        return Mono.just(convertToCompleteResult(result));
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
                || String.class.isAssignableFrom(returnType)
                || Mono.class.isAssignableFrom(returnType);

        if (!validReturnType) {
            throw new IllegalArgumentException(
                    String.format("Method must return either CompleteResult, CompleteCompletion, List<String>, " +
                                    "String, or Mono<T>: %s in %s returns %s",
                            method.getName(), method.getDeclaringClass().getName(), returnType.getName())
            );
        }
    }

    @Override
    protected boolean isExchangeType(Class<?> paramType) {
        return McpAsyncServerExchange.class.isAssignableFrom(paramType);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractBuilder<Builder, AsyncMcpCompleteMethodCallback> {
        public Builder() {
            this.uriTemplateManagerFactory = McpUriTemplateManager.DEFAULT_FACTORY;
        }

        @Override
        public AsyncMcpCompleteMethodCallback build() {
            validate();
            return new AsyncMcpCompleteMethodCallback(this);
        }
    }
}