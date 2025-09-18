package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

@SuppressWarnings("unused")
@FunctionalInterface
public interface McpServerSessionFactory {
    McpServerSession create(McpServerSessionTransport sessionTransport);
}
