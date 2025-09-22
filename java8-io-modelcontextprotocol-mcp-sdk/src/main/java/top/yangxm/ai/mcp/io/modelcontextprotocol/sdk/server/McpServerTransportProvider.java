package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

@SuppressWarnings("unused")
public interface McpServerTransportProvider extends McpServerTransportProviderBase {
    void setSessionFactory(McpServerSession.Factory sessionFactory);
}
