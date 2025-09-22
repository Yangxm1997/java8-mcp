package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.util.Lists;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.ProtocolVersions;

import java.util.List;

public interface McpStatelessServerTransport {
    void setMcpHandler(McpStatelessServerHandler mcpHandler);

    default void close() {
        this.closeGracefully().subscribe();
    }

    Mono<Void> closeGracefully();

    default List<String> protocolVersions() {
        return Lists.of(ProtocolVersions.MCP_2025_03_26, ProtocolVersions.MCP_2025_06_18);
    }
}
