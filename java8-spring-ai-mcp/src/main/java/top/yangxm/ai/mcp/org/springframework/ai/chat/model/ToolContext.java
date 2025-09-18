package top.yangxm.ai.mcp.org.springframework.ai.chat.model;

import top.yangxm.ai.mcp.org.springframework.ai.chat.messages.Message;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public final class ToolContext {
    public static final String TOOL_CALL_HISTORY = "TOOL_CALL_HISTORY";

    private final Map<String, Object> context;

    public ToolContext(Map<String, Object> context) {
        this.context = Collections.unmodifiableMap(context);
    }

    public Map<String, Object> getContext() {
        return this.context;
    }

    @SuppressWarnings("unchecked")
    public List<Message> getToolCallHistory() {
        return (List<Message>) this.context.get(TOOL_CALL_HISTORY);
    }
}