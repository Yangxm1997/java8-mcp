package top.yangxm.ai.mcp.org.springaicommunity.mcp.provider;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class ProviderUtils {
    public final static Predicate<Method> isReactiveReturnType = method -> Mono.class
            .isAssignableFrom(method.getReturnType()) || Flux.class.isAssignableFrom(method.getReturnType())
            || Publisher.class.isAssignableFrom(method.getReturnType());

    public final static Predicate<Method> isNotReactiveReturnType = method -> !Mono.class
            .isAssignableFrom(method.getReturnType()) && !Flux.class.isAssignableFrom(method.getReturnType())
            && !Publisher.class.isAssignableFrom(method.getReturnType());
}