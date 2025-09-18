package top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.spring.scan;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractAnnotatedMethodBeanPostProcessor implements BeanPostProcessor {
    private final AbstractMcpAnnotatedBeans registry;
    private final Set<Class<? extends Annotation>> targetAnnotations;

    public AbstractAnnotatedMethodBeanPostProcessor(AbstractMcpAnnotatedBeans registry,
                                                    Set<Class<? extends Annotation>> targetAnnotations) {
        Assert.notNull(registry, "AnnotatedBeanRegistry must not be null");
        Assert.notEmpty(targetAnnotations, "Target annotations must not be empty");
        this.registry = registry;
        this.targetAnnotations = targetAnnotations;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = AopUtils.getTargetClass(bean);
        Set<Class<? extends Annotation>> foundAnnotations = new HashSet<>();
        ReflectionUtils.doWithMethods(beanClass, method -> {
            this.targetAnnotations.forEach(annotationType -> {
                if (AnnotationUtils.findAnnotation(method, annotationType) != null) {
                    foundAnnotations.add(annotationType);
                }
            });
        });
        if (!foundAnnotations.isEmpty()) {
            this.registry.addMcpAnnotatedBean(bean, foundAnnotations);
        }
        return bean;
    }
}
