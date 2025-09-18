package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

@SuppressWarnings("unused")
public interface McpServerSessionTransportProvider extends McpServerTransportProvider {
    void setSessionFactory(McpServerSessionFactory sessionFactory);
}
