package top.yangxm.ai.mcp.org.springframework.ai.tool.metadata;

import org.springframework.util.Assert;
import top.yangxm.ai.mcp.org.springframework.ai.tool.support.ToolUtils;

import java.lang.reflect.Method;

@SuppressWarnings("unused")
public interface ToolMetadata {
    default boolean returnDirect() {
        return false;
    }

    static DefaultToolMetadata.Builder builder() {
        return DefaultToolMetadata.builder();
    }

    static ToolMetadata from(Method method) {
        Assert.notNull(method, "method cannot be null");
        return DefaultToolMetadata.builder().returnDirect(ToolUtils.getToolReturnDirect(method)).build();
    }
}
