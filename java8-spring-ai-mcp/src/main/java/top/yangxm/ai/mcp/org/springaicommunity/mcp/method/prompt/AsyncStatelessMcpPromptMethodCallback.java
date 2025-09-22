package top.yangxm.ai.mcp.org.springaicommunity.mcp.method.prompt;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common.McpTransportContext;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.GetPromptRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.GetPromptResult;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

@SuppressWarnings("unused")
public final class AsyncStatelessMcpPromptMethodCallback
        extends AbstractMcpPromptMethodCallback
        implements BiFunction<McpTransportContext, GetPromptRequest, Mono<GetPromptResult>> {

    private AsyncStatelessMcpPromptMethodCallback(Builder builder) {
        super(builder.method, builder.bean, builder.prompt);
    }

    @Override
    public Mono<GetPromptResult> apply(McpTransportContext context, GetPromptRequest request) {
        if (request == null) {
            return Mono.error(new IllegalArgumentException("Request must not be null"));
        }

        return Mono.defer(() -> {
            try {
                Object[] args = this.buildArgs(this.method, context, request);
                this.method.setAccessible(true);
                Object result = this.method.invoke(this.bean, args);

                if (result instanceof Mono<?>) {
                    return ((Mono<?>) result).map(this::convertToGetPromptResult);
                } else {
                    return Mono.just(convertToGetPromptResult(result));
                }
            } catch (Exception e) {
                return Mono.error(new McpPromptMethodException("Error invoking prompt method: " + this.method.getName(), e));
            }
        });
    }

    @Override
    protected boolean isExchangeOrContextType(Class<?> paramType) {
        return McpTransportContext.class.isAssignableFrom(paramType);
    }

    @Override
    protected void validateReturnType(Method method) {
        Class<?> returnType = method.getReturnType();
        if (!Mono.class.isAssignableFrom(returnType)) {
            throw new IllegalArgumentException(
                    String.format("Method must return a Mono<T> where T is one of GetPromptResult, List<PromptMessage>, " +
                                    "List<String>, PromptMessage, or String: %s in %s returns %s",
                            method.getName(), method.getDeclaringClass().getName(), returnType.getName())
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends AbstractBuilder<Builder, AsyncStatelessMcpPromptMethodCallback> {
        @Override
        public AsyncStatelessMcpPromptMethodCallback build() {
            validate();
            return new AsyncStatelessMcpPromptMethodCallback(this);
        }
    }
}

