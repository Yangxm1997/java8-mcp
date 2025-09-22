package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.server.transport;

public class McpTransportConst {
    public static final String DEFAULT_BASE_URL = "";
    public static final String DEFAULT_SSE_ENDPOINT = "/sse";
    public static final String MESSAGE_EVENT_TYPE = "message";
    public static final String ENDPOINT_EVENT_TYPE = "endpoint";

    public static final String DEFAULT_STATELESS_ENDPOINT = "/mcp";
    public static final String DEFAULT_STREAMABLE_ENDPOINT = "/mcp";

    public static final String HEADER_MCP_SESSION_ID = "mcp-session-id";
    public static final String HEADER_LAST_EVENT_ID = "Last-Event-ID";
}
