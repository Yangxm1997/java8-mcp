package top.yangxm.ai.mcp.org.springaicommunity.mcp.method.resource;

import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.ReadResourceResult;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.method.resource.AbstractMcpResourceMethodCallback.ContentType;

@SuppressWarnings("unused")
public interface McpReadResourceResultConverter {
    ReadResourceResult convertToReadResourceResult(Object result, String requestUri, String mimeType, ContentType contentType);
}
