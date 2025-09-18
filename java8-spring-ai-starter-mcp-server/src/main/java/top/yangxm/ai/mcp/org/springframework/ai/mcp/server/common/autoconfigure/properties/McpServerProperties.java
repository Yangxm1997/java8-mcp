package top.yangxm.ai.mcp.org.springframework.ai.mcp.server.common.autoconfigure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@ConfigurationProperties(McpServerProperties.CONFIG_PREFIX)
public class McpServerProperties {
    public static final String CONFIG_PREFIX = "spring.ai.mcp.server";

    private boolean enabled = true;
    private boolean stdio = false;
    private String name = "mcp-server";
    private String version = "1.0.0";
    private String instructions = null;
    private ApiType type = ApiType.ASYNC;
    private Capabilities capabilities = new Capabilities();
    private ServerProtocol protocol = ServerProtocol.SSE;
    private Duration requestTimeout = Duration.ofSeconds(20);
    private Map<String, String> toolResponseMimeType = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isStdio() {
        return stdio;
    }

    public void setStdio(boolean stdio) {
        this.stdio = stdio;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        Assert.hasText(name, "Name must not be empty");
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        Assert.hasText(version, "Version must not be empty");
        this.version = version;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public ApiType getType() {
        return type;
    }

    public void setType(ApiType type) {
        Assert.notNull(type, "Server type must not be null");
        this.type = type;
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Capabilities capabilities) {
        this.capabilities = capabilities;
    }

    public ServerProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(ServerProtocol protocol) {
        Assert.notNull(protocol, "Server protocol must not be null");
        this.protocol = protocol;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        Assert.notNull(requestTimeout, "Request timeout must not be null");
        this.requestTimeout = requestTimeout;
    }

    public Map<String, String> getToolResponseMimeType() {
        return toolResponseMimeType;
    }

    public void setToolResponseMimeType(Map<String, String> toolResponseMimeType) {
        this.toolResponseMimeType = toolResponseMimeType;
    }

    @Override
    public String toString() {
        return "McpServerProperties{" +
                "enabled=" + enabled +
                ", stdio=" + stdio +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", instructions='" + instructions + '\'' +
                ", type=" + type +
                ", capabilities=" + capabilities +
                ", protocol=" + protocol +
                ", requestTimeout=" + requestTimeout +
                ", toolResponseMimeType=" + toolResponseMimeType +
                '}';
    }

    public enum ApiType {
        // SYNC, // TODO: not support yet
        ASYNC
    }

    public enum ServerProtocol {
        SSE,
        // STREAMABLE, // TODO: not support yet
        // STATELESS // TODO: not support yet
    }

    public static class Capabilities {
        private boolean resource = true;
        private boolean tool = true;
        private boolean prompt = true;
        private boolean completion = true;

        public boolean isResource() {
            return this.resource;
        }

        public void setResource(boolean resource) {
            this.resource = resource;
        }

        public boolean isTool() {
            return this.tool;
        }

        public void setTool(boolean tool) {
            this.tool = tool;
        }

        public boolean isPrompt() {
            return this.prompt;
        }

        public void setPrompt(boolean prompt) {
            this.prompt = prompt;
        }

        public boolean isCompletion() {
            return this.completion;
        }

        public void setCompletion(boolean completion) {
            this.completion = completion;
        }

        @Override
        public String toString() {
            return "Capabilities{" +
                    "resource=" + resource +
                    ", tool=" + tool +
                    ", prompt=" + prompt +
                    ", completion=" + completion +
                    '}';
        }
    }
}
