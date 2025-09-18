package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.json.TypeRef;
import top.yangxm.ai.mcp.commons.util.Lists;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCMessage;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.ProtocolVersions;

import java.util.List;

@SuppressWarnings("unused")
public interface McpTransport {
    Mono<Void> sendMessage(JSONRPCMessage message);

    Mono<Void> closeGracefully();

    default void close() {
        this.closeGracefully().subscribe();
    }

    <T> T unmarshalFrom(Object data, TypeRef<T> typeRef);

    default List<String> protocolVersions() {
        return Lists.of(ProtocolVersions.suggestedVersion());
    }
}