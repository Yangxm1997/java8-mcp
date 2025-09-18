package top.yangxm.ai.mcp.org.springaicommunity.mcp.method.tool.utils;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.CallToolResult;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unused")
public final class ReactiveUtils {
    private ReactiveUtils() {
    }

    private static final Map<Type, Boolean> isReactiveOfVoidCache = new ConcurrentReferenceHashMap<>(256);
    private static final Map<Type, Boolean> isReactiveOfCallToolResultCache = new ConcurrentReferenceHashMap<>(256);

    public static boolean isReactiveReturnTypeOfVoid(Method method) {
        Type returnType = method.getGenericReturnType();
        if (isReactiveOfVoidCache.containsKey(returnType)) {
            return isReactiveOfVoidCache.get(returnType);
        }

        boolean isReactiveOfVoid = false;
        if (returnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) returnType;
            Type rawType = parameterizedType.getRawType();

            if (rawType instanceof Class) {
                Class<?> rawClass = (Class<?>) rawType;
                if (Mono.class.isAssignableFrom(rawClass)
                        || Flux.class.isAssignableFrom(rawClass)
                        || Publisher.class.isAssignableFrom(rawClass)) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length == 1) {
                        Type typeArgument = typeArguments[0];
                        if (typeArgument instanceof Class) {
                            isReactiveOfVoid = Void.class.equals(typeArgument) || void.class.equals(typeArgument);
                        }
                    }
                }
            }
        }
        isReactiveOfVoidCache.putIfAbsent(returnType, isReactiveOfVoid);
        return isReactiveOfVoid;
    }

    public static boolean isReactiveReturnTypeOfCallToolResult(Method method) {
        Type returnType = method.getGenericReturnType();
        if (isReactiveOfCallToolResultCache.containsKey(returnType)) {
            return isReactiveOfCallToolResultCache.get(returnType);
        }
        boolean isReactiveOfCallToolResult = false;
        if (returnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) returnType;
            Type rawType = parameterizedType.getRawType();

            if (rawType instanceof Class) {
                Class<?> rawClass = (Class<?>) rawType;
                if (Mono.class.isAssignableFrom(rawClass)
                        || Flux.class.isAssignableFrom(rawClass)
                        || Publisher.class.isAssignableFrom(rawClass)) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length == 1) {
                        Type typeArgument = typeArguments[0];
                        if (typeArgument instanceof Class) {
                            isReactiveOfCallToolResult = CallToolResult.class.isAssignableFrom((Class<?>) typeArgument);
                        }
                    }
                }
            }
        }
        isReactiveOfCallToolResultCache.putIfAbsent(returnType, isReactiveOfCallToolResult);
        return isReactiveOfCallToolResult;
    }

    public static Optional<Type> getReactiveReturnTypeArgument(Method method) {
        Type returnType = method.getGenericReturnType();
        if (returnType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) returnType;
            Type rawType = parameterizedType.getRawType();

            if (rawType instanceof Class) {
                Class<?> rawClass = (Class<?>) rawType;
                if (Mono.class.isAssignableFrom(rawClass)
                        || Flux.class.isAssignableFrom(rawClass)
                        || Publisher.class.isAssignableFrom(rawClass)) {
                    return Optional.of(parameterizedType.getActualTypeArguments()[0]);
                }
            }
        }
        return Optional.empty();
    }
}
