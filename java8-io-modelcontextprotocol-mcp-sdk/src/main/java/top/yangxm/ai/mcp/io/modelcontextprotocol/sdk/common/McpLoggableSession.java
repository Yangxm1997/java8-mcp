package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common;

import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema.LoggingLevel;

public interface McpLoggableSession extends McpSession {
    void setMinLoggingLevel(LoggingLevel minLoggingLevel);

    boolean isNotificationForLevelAllowed(LoggingLevel loggingLevel);
}
