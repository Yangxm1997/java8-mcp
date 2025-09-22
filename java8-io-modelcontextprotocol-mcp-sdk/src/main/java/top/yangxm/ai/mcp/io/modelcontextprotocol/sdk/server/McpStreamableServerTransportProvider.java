package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

@SuppressWarnings("unused")
public interface McpStreamableServerTransportProvider extends McpServerTransportProviderBase {
    void setSessionFactory(McpStreamableServerSession.Factory sessionFactory);
}
