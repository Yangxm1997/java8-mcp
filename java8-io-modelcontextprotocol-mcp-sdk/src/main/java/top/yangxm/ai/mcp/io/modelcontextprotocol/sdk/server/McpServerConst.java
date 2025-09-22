package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server;

import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.Implementation;

import java.time.Duration;

final class McpServerConst {
    private McpServerConst() {
    }

    final static Implementation DEFAULT_SERVER_INFO = new Implementation("mcp-server", "1.0.0");
    final static Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofHours(10);
}
