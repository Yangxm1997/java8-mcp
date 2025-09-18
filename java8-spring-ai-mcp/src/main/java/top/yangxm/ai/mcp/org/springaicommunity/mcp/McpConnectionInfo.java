package top.yangxm.ai.mcp.org.springaicommunity.mcp;

import top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.schema.McpSchema;

@SuppressWarnings("unused")
public final class McpConnectionInfo {
    private final McpSchema.ClientCapabilities clientCapabilities;
    private final McpSchema.Implementation clientInfo;
    private final McpSchema.InitializeResult initializeResult;

    public McpConnectionInfo(McpSchema.ClientCapabilities clientCapabilities,
                             McpSchema.Implementation clientInfo,
                             McpSchema.InitializeResult initializeResult) {
        this.clientCapabilities = clientCapabilities;
        this.clientInfo = clientInfo;
        this.initializeResult = initializeResult;
    }

    public McpSchema.ClientCapabilities clientCapabilities() {
        return clientCapabilities;
    }

    public McpSchema.Implementation clientInfo() {
        return clientInfo;
    }

    public McpSchema.InitializeResult initializeResult() {
        return initializeResult;
    }

    @Override
    public String toString() {
        return "McpConnectionInfo{" +
                "clientCapabilities=" + clientCapabilities +
                ", clientInfo=" + clientInfo +
                ", initializeResult=" + initializeResult +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private McpSchema.ClientCapabilities clientCapabilities;
        private McpSchema.Implementation clientInfo;
        private McpSchema.InitializeResult initializeResult;

        private Builder() {
        }

        public Builder clientCapabilities(McpSchema.ClientCapabilities clientCapabilities) {
            this.clientCapabilities = clientCapabilities;
            return this;
        }

        public Builder clientInfo(McpSchema.Implementation clientInfo) {
            this.clientInfo = clientInfo;
            return this;
        }

        public Builder initializeResult(McpSchema.InitializeResult initializeResult) {
            this.initializeResult = initializeResult;
            return this;
        }

        public McpConnectionInfo build() {
            return new McpConnectionInfo(this.clientCapabilities, this.clientInfo, this.initializeResult);
        }
    }
}
