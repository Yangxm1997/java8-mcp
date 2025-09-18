package top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.spring.scan;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractMcpAnnotatedBeans {
    private final List<Object> beansWithCustomAnnotations = new ArrayList<>();

    private final Map<Class<? extends Annotation>, List<Object>> beansByAnnotation = new HashMap<>();

    public void addMcpAnnotatedBean(Object bean, Set<Class<? extends Annotation>> annotations) {
        this.beansWithCustomAnnotations.add(bean);
        annotations.forEach(annotationType ->
                this.beansByAnnotation.computeIfAbsent(annotationType, k -> new ArrayList<>()).add(bean)
        );
    }

    public List<Object> getAllAnnotatedBeans() {
        return new ArrayList<>(this.beansWithCustomAnnotations);
    }

    public List<Object> getBeansByAnnotation(Class<? extends Annotation> annotationType) {
        return this.beansByAnnotation.getOrDefault(annotationType, Collections.emptyList());
    }

    public int getCount() {
        return this.beansWithCustomAnnotations.size();
    }
}
