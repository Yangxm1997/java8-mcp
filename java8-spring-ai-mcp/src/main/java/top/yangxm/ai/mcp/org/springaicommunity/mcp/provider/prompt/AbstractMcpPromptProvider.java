package top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.prompt;

import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.List;

@SuppressWarnings("unused")
public abstract class AbstractMcpPromptProvider {
    protected final List<Object> promptObjects;

    public AbstractMcpPromptProvider(List<Object> promptObjects) {
        Assert.notNull(promptObjects, "promptObjects cannot be null");
        this.promptObjects = promptObjects;
    }

    protected Method[] doGetClassMethods(Object bean) {
        return bean.getClass().getDeclaredMethods();
    }
}