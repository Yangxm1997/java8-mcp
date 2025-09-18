package top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@SuppressWarnings("unused")
@ConfigurationProperties(McpServerChangeNotificationProperties.CONFIG_PREFIX)
public class McpServerChangeNotificationProperties {
    public static final String CONFIG_PREFIX = "spring.ai.mcp.server";

    private boolean resourceChangeNotification = true;
    private boolean toolChangeNotification = true;
    private boolean promptChangeNotification = true;

    public boolean isResourceChangeNotification() {
        return resourceChangeNotification;
    }

    public void setResourceChangeNotification(boolean resourceChangeNotification) {
        this.resourceChangeNotification = resourceChangeNotification;
    }

    public boolean isToolChangeNotification() {
        return toolChangeNotification;
    }

    public void setToolChangeNotification(boolean toolChangeNotification) {
        this.toolChangeNotification = toolChangeNotification;
    }

    public boolean isPromptChangeNotification() {
        return promptChangeNotification;
    }

    public void setPromptChangeNotification(boolean promptChangeNotification) {
        this.promptChangeNotification = promptChangeNotification;
    }

    @Override
    public String toString() {
        return "McpServerChangeNotificationProperties{" +
                "resourceChangeNotification=" + resourceChangeNotification +
                ", toolChangeNotification=" + toolChangeNotification +
                ", promptChangeNotification=" + promptChangeNotification +
                '}';
    }
}
