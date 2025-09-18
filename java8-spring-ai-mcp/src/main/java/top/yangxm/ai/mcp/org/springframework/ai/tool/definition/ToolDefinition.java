package top.yangxm.ai.mcp.org.springframework.ai.tool.definition;

@SuppressWarnings("unused")
public interface ToolDefinition {
    String name();

    String description();

    String inputSchema();

    static DefaultToolDefinition.Builder builder() {
        return DefaultToolDefinition.builder();
    }
}
