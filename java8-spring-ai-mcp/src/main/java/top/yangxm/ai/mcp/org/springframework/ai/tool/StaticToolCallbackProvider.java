package top.yangxm.ai.mcp.org.springframework.ai.tool;

import org.springframework.util.Assert;

import java.util.List;

@SuppressWarnings("unused")
public class StaticToolCallbackProvider implements ToolCallbackProvider {
    private final ToolCallback[] toolCallbacks;

    public StaticToolCallbackProvider(ToolCallback... toolCallbacks) {
        Assert.notNull(toolCallbacks, "ToolCallbacks must not be null");
        this.toolCallbacks = toolCallbacks;
    }

    public StaticToolCallbackProvider(List<? extends ToolCallback> toolCallbacks) {
        Assert.noNullElements(toolCallbacks, "toolCallbacks cannot contain null elements");
        this.toolCallbacks = toolCallbacks.toArray(new ToolCallback[0]);
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        return this.toolCallbacks;
    }
}
