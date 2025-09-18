package top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.spring;

import org.springframework.aop.support.AopUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public final class AnnotationProviderUtil {
    private AnnotationProviderUtil() {
    }

    public static Method[] beanMethods(Object bean) {
        Method[] methods = ReflectionUtils.getUniqueDeclaredMethods
                (AopUtils.isAopProxy(bean) ? AopUtils.getTargetClass(bean) : bean.getClass());
        methods = Stream.of(methods).filter(ReflectionUtils.USER_DECLARED_METHODS::matches).toArray(Method[]::new);
        Arrays.sort(methods, Comparator.comparing(Method::getName)
                .thenComparing(method -> Arrays.toString(method.getParameterTypes())));
        return methods;
    }
}
