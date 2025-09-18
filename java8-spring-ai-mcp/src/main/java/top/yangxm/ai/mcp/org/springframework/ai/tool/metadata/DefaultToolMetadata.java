package top.yangxm.ai.mcp.org.springframework.ai.tool.metadata;

@SuppressWarnings("unused")
public final class DefaultToolMetadata implements ToolMetadata {
    private final boolean returnDirect;

    public DefaultToolMetadata(boolean returnDirect) {
        this.returnDirect = returnDirect;
    }

    @Override
    public boolean returnDirect() {
        return returnDirect;
    }

    @Override
    public String toString() {
        return "DefaultToolMetadata{" +
                "returnDirect=" + returnDirect +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean returnDirect = false;

        private Builder() {
        }

        public Builder returnDirect(boolean returnDirect) {
            this.returnDirect = returnDirect;
            return this;
        }

        public ToolMetadata build() {
            return new DefaultToolMetadata(this.returnDirect);
        }
    }
}
