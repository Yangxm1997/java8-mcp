package top.yangxm.ai.mcp.org.springframework.ai.tool.support;

import org.springframework.util.Assert;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.method.tool.utils.ToolJsonSchemaGenerator;
import top.yangxm.ai.mcp.org.springframework.ai.tool.definition.DefaultToolDefinition;
import top.yangxm.ai.mcp.org.springframework.ai.tool.definition.ToolDefinition;

import java.lang.reflect.Method;

@SuppressWarnings("unused")
public final class ToolDefinitions {
    private ToolDefinitions() {
    }

    public static DefaultToolDefinition.Builder builder(Method method) {
        Assert.notNull(method, "method cannot be null");
        return DefaultToolDefinition.builder()
                .name(ToolUtils.getToolName(method))
                .description(ToolUtils.getToolDescription(method))
                .inputSchema(ToolJsonSchemaGenerator.generateForMethodInput(method));
    }

    public static ToolDefinition from(Method method) {
        return builder(method).build();
    }
}
