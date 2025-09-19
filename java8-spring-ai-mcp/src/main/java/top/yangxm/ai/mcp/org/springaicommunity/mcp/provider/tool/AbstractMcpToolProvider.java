package top.yangxm.ai.mcp.org.springaicommunity.mcp.provider.tool;

import org.springframework.util.Assert;
import top.yangxm.ai.mcp.commons.json.JsonMapper;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpTool;

import java.lang.reflect.Method;
import java.util.List;

@SuppressWarnings("unused")
public abstract class AbstractMcpToolProvider {
    protected final List<Object> toolObjects;

    protected JsonMapper jsonMapper = JsonMapper.getDefault();

    public AbstractMcpToolProvider(List<Object> toolObjects) {
        Assert.notNull(toolObjects, "toolObjects cannot be null");
        this.toolObjects = toolObjects;
    }

    protected Method[] doGetClassMethods(Object bean) {
        return bean.getClass().getDeclaredMethods();
    }

    protected McpTool doGetMcpToolAnnotation(Method method) {
        return method.getAnnotation(McpTool.class);
    }

    protected Class<? extends Throwable> doGetToolCallException() {
        return Exception.class;
    }

    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public JsonMapper getJsonMapper() {
        return this.jsonMapper;
    }
}
