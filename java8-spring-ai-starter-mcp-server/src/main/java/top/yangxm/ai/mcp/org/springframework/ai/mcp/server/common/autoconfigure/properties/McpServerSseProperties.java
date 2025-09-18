package top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.time.Duration;

@SuppressWarnings("unused")
@ConfigurationProperties(McpServerSseProperties.CONFIG_PREFIX)
public class McpServerSseProperties {
    public static final String CONFIG_PREFIX = "spring.ai.mcp.server";

    private String baseUrl = "";
    private String sseEndpoint = "/sse";
    private String sseMessageEndpoint = "/mcp/message";
    private Duration keepAliveInterval;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        Assert.notNull(baseUrl, "Base URL must not be null");
        this.baseUrl = baseUrl;
    }

    public String getSseEndpoint() {
        return sseEndpoint;
    }

    public void setSseEndpoint(String sseEndpoint) {
        Assert.hasText(sseEndpoint, "SSE endpoint must not be empty");
        this.sseEndpoint = sseEndpoint;
    }

    public String getSseMessageEndpoint() {
        return sseMessageEndpoint;
    }

    public void setSseMessageEndpoint(String sseMessageEndpoint) {
        Assert.hasText(sseMessageEndpoint, "SSE message endpoint must not be empty");
        this.sseMessageEndpoint = sseMessageEndpoint;
    }

    public Duration getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public void setKeepAliveInterval(Duration keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    @Override
    public String toString() {
        return "McpServerSseProperties{" +
                "baseUrl='" + baseUrl + '\'' +
                ", sseEndpoint='" + sseEndpoint + '\'' +
                ", sseMessageEndpoint='" + sseMessageEndpoint + '\'' +
                ", keepAliveInterval=" + keepAliveInterval +
                '}';
    }
}
