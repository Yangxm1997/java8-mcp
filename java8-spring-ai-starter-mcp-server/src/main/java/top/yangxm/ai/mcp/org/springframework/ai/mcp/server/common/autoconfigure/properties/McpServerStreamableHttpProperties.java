package top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@SuppressWarnings("unused")
@ConfigurationProperties(McpServerStreamableHttpProperties.CONFIG_PREFIX)
public class McpServerStreamableHttpProperties {
    public static final String CONFIG_PREFIX = "spring.ai.mcp.server.streamable-http";

    private String mcpEndpoint = "/mcp";

    private Duration keepAliveInterval;

    private boolean disallowDelete;

    public String getMcpEndpoint() {
        return mcpEndpoint;
    }

    public void setMcpEndpoint(String mcpEndpoint) {
        this.mcpEndpoint = mcpEndpoint;
    }

    public Duration getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public void setKeepAliveInterval(Duration keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    public boolean isDisallowDelete() {
        return disallowDelete;
    }

    public void setDisallowDelete(boolean disallowDelete) {
        this.disallowDelete = disallowDelete;
    }

    @Override
    public String toString() {
        return "McpServerStreamableHttpProperties{" +
                "mcpEndpoint='" + mcpEndpoint + '\'' +
                ", keepAliveInterval=" + keepAliveInterval +
                ", disallowDelete=" + disallowDelete +
                '}';
    }
}