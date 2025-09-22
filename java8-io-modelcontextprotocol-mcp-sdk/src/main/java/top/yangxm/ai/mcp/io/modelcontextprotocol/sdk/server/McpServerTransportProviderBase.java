package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.util.Lists;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.ProtocolVersions;

import java.util.List;

@SuppressWarnings("unused")
public interface McpServerTransportProviderBase {
    Mono<Void> notifyClients(String method, Object params);

    default void close() {
        this.closeGracefully().subscribe();
    }

    Mono<Void> closeGracefully();

    default List<String> protocolVersions() {
        return Lists.of(ProtocolVersions.suggestedVersion());
    }
}
