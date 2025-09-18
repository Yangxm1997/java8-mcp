package top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpResource;

import java.lang.reflect.Method;
import java.util.List;

@SuppressWarnings("unused")
public abstract class AbstractMcpResourceProvider {
    protected final List<Object> resourceObjects;

    public AbstractMcpResourceProvider(List<Object> resourceObjects) {
        Assert.notNull(resourceObjects, "resourceObjects cannot be null");
        this.resourceObjects = resourceObjects;
    }

    protected Method[] doGetClassMethods(Object bean) {
        return bean.getClass().getDeclaredMethods();
    }

    protected McpResource doGetMcpResourceAnnotation(Method method) {
        return method.getAnnotation(McpResource.class);
    }

    static String getName(Method method, McpResource resource) {
        Assert.notNull(method, "method cannot be null");
        if (resource == null || StringUtils.isBlank(resource.name())) {
            return method.getName();
        }
        return resource.name();
    }
}
