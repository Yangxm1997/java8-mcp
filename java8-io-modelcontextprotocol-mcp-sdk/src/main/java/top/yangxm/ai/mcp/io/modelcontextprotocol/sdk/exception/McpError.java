package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.exception;

import top.yangxm.ai.mcp.commons.util.Assert;
import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.JSONRPCError;

@SuppressWarnings("unused")
public class McpError extends RuntimeException {
    private final JSONRPCError jsonRpcError;

    public McpError(JSONRPCError jsonRpcError) {
        super(jsonRpcError.message());
        this.jsonRpcError = jsonRpcError;
    }

    public JSONRPCError jsonRpcError() {
        return jsonRpcError;
    }

    public static McpError of(String message) {
        return of(0, message, null);
    }

    public static McpError of(int code, String message) {
        return of(code, message, null);
    }

    public static McpError of(int code, String message, Object data) {
        Assert.hasText(message, "message must not be empty");
        return new McpError(new JSONRPCError(code, message, data));
    }
}
