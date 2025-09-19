package top.yangxm.ai.mcp.org.springaicommunity.mcp.method.tool;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.json.JsonMapper;
import top.yangxm.ai.mcp.commons.json.TypeRef;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CallToolRequest;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CallToolResult;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpMeta;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpProgressToken;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.method.tool.utils.ReactiveUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public abstract class AbstractAsyncMcpToolMethodCallback<T> {
    private static final JsonMapper JSON_MAPPER = JsonMapper.getDefault();
    private static final TypeRef<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeRef<Map<String, Object>>() {
    };

    protected final ReturnMode returnMode;
    protected final Method toolMethod;
    protected final Object toolObject;
    protected final Class<? extends Throwable> toolCallExceptionClass;

    protected AbstractAsyncMcpToolMethodCallback(ReturnMode returnMode,
                                                 Method toolMethod,
                                                 Object toolObject,
                                                 Class<? extends Throwable> toolCallExceptionClass) {
        this.returnMode = returnMode;
        this.toolMethod = toolMethod;
        this.toolObject = toolObject;
        this.toolCallExceptionClass = toolCallExceptionClass;
    }

    protected Object callMethod(Object[] methodArguments) {
        this.toolMethod.setAccessible(true);
        Object result;
        try {
            result = this.toolMethod.invoke(this.toolObject, methodArguments);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Could not access method: " + ex.getMessage(), ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException("Error invoking method: " + this.toolMethod.getName(), ex);
        }
        return result;
    }

    protected Object[] buildMethodArguments(T exchangeOrContext,
                                            Map<String, Object> toolInputArguments,
                                            CallToolRequest request) {
        return Stream.of(this.toolMethod.getParameters()).map(parameter -> {
            if (parameter.isAnnotationPresent(McpProgressToken.class)) {
                return request != null ? request.progressToken() : null;
            }

            if (McpMeta.class.isAssignableFrom(parameter.getType())) {
                return request != null ? new McpMeta(request.meta()) : new McpMeta(null);
            }

            if (CallToolRequest.class.isAssignableFrom(parameter.getType())) {
                return request;
            }

            if (isExchangeOrContextType(parameter.getType())) {
                return exchangeOrContext;
            }

            Object rawArgument = toolInputArguments.get(parameter.getName());
            return buildTypedArgument(rawArgument, parameter.getParameterizedType());
        }).toArray();
    }

    protected Object buildTypedArgument(Object value, Type type) {
        if (value == null) {
            return null;
        }

        if (type instanceof Class<?>) {
            return JsonMapper.toTypedObject(value, (Class<?>) type);
        }

        String json = JSON_MAPPER.writeValueAsString(value);
        return JSON_MAPPER.readValue(json, type);
    }

    @SuppressWarnings("unchecked")
    protected Mono<CallToolResult> convertToCallToolResult(Object result) {
        if (result instanceof Mono) {
            Mono<?> monoResult = (Mono<?>) result;
            if (ReactiveUtils.isReactiveReturnTypeOfCallToolResult(this.toolMethod)) {
                return (Mono<CallToolResult>) monoResult;
            }

            if (ReactiveUtils.isReactiveReturnTypeOfVoid(this.toolMethod)) {
                return monoResult
                        .then(Mono.just(CallToolResult.builder().addTextContent(JSON_MAPPER.writeValueAsString("Done")).build()));
            }

            return monoResult.map(this::mapValueToCallToolResult)
                    .onErrorResume(e -> Mono.just(CallToolResult.builder()
                            .isError(true)
                            .addTextContent(String.format("Error invoking method: %s", e.getMessage()))
                            .build()));
        }

        if (result instanceof Flux) {
            Flux<?> fluxResult = (Flux<?>) result;
            if (ReactiveUtils.isReactiveReturnTypeOfCallToolResult(this.toolMethod)) {
                return ((Flux<CallToolResult>) fluxResult).next();
            }

            if (ReactiveUtils.isReactiveReturnTypeOfVoid(this.toolMethod)) {
                return fluxResult
                        .then(Mono.just(CallToolResult.builder().addTextContent(JSON_MAPPER.writeValueAsString("Done")).build()));
            }

            return fluxResult.next()
                    .map(this::mapValueToCallToolResult)
                    .onErrorResume(e -> Mono.just(CallToolResult.builder()
                            .isError(true)
                            .addTextContent(String.format("Error invoking method: %s", e.getMessage()))
                            .build()));
        }

        if (result instanceof Publisher) {
            Publisher<?> publisherResult = (Publisher<?>) result;
            Mono<?> monoFromPublisher = Mono.from(publisherResult);

            if (ReactiveUtils.isReactiveReturnTypeOfCallToolResult(this.toolMethod)) {
                return (Mono<CallToolResult>) monoFromPublisher;
            }

            if (ReactiveUtils.isReactiveReturnTypeOfVoid(this.toolMethod)) {
                return monoFromPublisher
                        .then(Mono.just(CallToolResult.builder().addTextContent(JSON_MAPPER.writeValueAsString("Done")).build()));
            }

            return monoFromPublisher.map(this::mapValueToCallToolResult)
                    .onErrorResume(e -> Mono.just(CallToolResult.builder()
                            .isError(true)
                            .addTextContent(String.format("Error invoking method: %s", e.getMessage()))
                            .build()));
        }

        throw new IllegalStateException("Expected reactive return type but got: "
                + (result != null ? result.getClass().getName() : "null"));
    }

    protected CallToolResult mapValueToCallToolResult(Object value) {
        if (value instanceof CallToolResult) {
            return (CallToolResult) value;
        }

        Type returnType = this.toolMethod.getGenericReturnType();
        if (returnMode == ReturnMode.VOID || returnType == void.class) {
            return CallToolResult.builder().addTextContent(JSON_MAPPER.writeValueAsString("Done")).build();
        }

        if (this.returnMode == ReturnMode.STRUCTURED) {
            Map<String, Object> structuredOutput = JSON_MAPPER.convertValue(value, MAP_TYPE_REFERENCE);
            return CallToolResult.builder().structuredContent(structuredOutput).build();
        }

        if (value == null) {
            return CallToolResult.builder().addTextContent("null").build();
        }

        if (value instanceof String) {
            return CallToolResult.builder().addTextContent((String) value).build();
        }

        return CallToolResult.builder().addTextContent(JSON_MAPPER.writeValueAsString(value)).build();
    }

    protected Mono<CallToolResult> createErrorResult(Exception e) {
        return Mono.just(CallToolResult.builder()
                .isError(true)
                .addTextContent(String.format("Error invoking method: %s", e.getMessage()))
                .build());
    }

    protected Mono<Void> validateRequest(CallToolRequest request) {
        if (request == null) {
            return Mono.error(new IllegalArgumentException("Request must not be null"));
        }
        return Mono.empty();
    }

    protected abstract boolean isExchangeOrContextType(Class<?> paramType);
}
