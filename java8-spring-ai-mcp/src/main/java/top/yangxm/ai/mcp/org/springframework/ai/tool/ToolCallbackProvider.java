package top.yangxm.ai.mcp.org.springframework.ai.tool;

import java.util.List;

@SuppressWarnings("unused")
public interface ToolCallbackProvider {

    ToolCallback[] getToolCallbacks();

    static ToolCallbackProvider from(List<? extends ToolCallback> toolCallbacks) {
        return new StaticToolCallbackProvider(toolCallbacks);
    }

    static ToolCallbackProvider from(ToolCallback... toolCallbacks) {
        return new StaticToolCallbackProvider(toolCallbacks);
    }
}
