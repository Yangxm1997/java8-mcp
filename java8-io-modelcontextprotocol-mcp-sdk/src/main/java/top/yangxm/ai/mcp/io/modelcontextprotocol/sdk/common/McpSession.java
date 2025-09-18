package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common;

import reactor.core.publisher.Mono;
import top.yangxm.ai.mcp.commons.json.TypeRef;

@SuppressWarnings("unused")
public interface McpSession {
    <T> Mono<T> sendRequest(String method, Object requestParams, TypeRef<T> typeRef);

    default Mono<Void> sendNotification(String method) {
        return sendNotification(method, null);
    }

    Mono<Void> sendNotification(String method, Object params);

    Mono<Void> closeGracefully();

    void close();
}
