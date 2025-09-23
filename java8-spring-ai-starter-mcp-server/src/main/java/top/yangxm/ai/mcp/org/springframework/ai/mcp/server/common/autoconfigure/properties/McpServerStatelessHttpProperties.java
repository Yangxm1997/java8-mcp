package top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@SuppressWarnings("unused")
@ConfigurationProperties(McpServerStatelessHttpProperties.CONFIG_PREFIX)
public class McpServerStatelessHttpProperties {
    public static final String CONFIG_PREFIX = "spring.ai.mcp.server.stateless-http";

    private String mcpEndpoint = "/mcp";

    public String getMcpEndpoint() {
        return mcpEndpoint;
    }

    public void setMcpEndpoint(String mcpEndpoint) {
        this.mcpEndpoint = mcpEndpoint;
    }

    @Override
    public String toString() {
        return "McpServerStatelessHttpProperties{" +
                "mcpEndpoint='" + mcpEndpoint + '\'' +
                '}';
    }
}