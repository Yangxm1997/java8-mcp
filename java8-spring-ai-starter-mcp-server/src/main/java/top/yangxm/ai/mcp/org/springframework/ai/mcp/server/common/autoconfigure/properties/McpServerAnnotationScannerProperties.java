package top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@SuppressWarnings("unused")
@ConfigurationProperties(prefix = McpServerAnnotationScannerProperties.CONFIG_PREFIX)
public class McpServerAnnotationScannerProperties {
    public static final String CONFIG_PREFIX = "spring.ai.mcp.server.annotation-scanner";
    private boolean enabled = true;

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "McpServerAnnotationScannerProperties{" +
                "enabled=" + enabled +
                '}';
    }
}
