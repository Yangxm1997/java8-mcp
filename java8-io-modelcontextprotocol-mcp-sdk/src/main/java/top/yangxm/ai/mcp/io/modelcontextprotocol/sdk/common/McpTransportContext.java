package top.yangxm.ai.mcp.io.modelcontextprotocol.sdk.common;

import top.yangxm.ai.mcp.commons.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public interface McpTransportContext {
    String KEY = "MCP_TRANSPORT_CONTEXT";

    McpTransportContext EMPTY = new DefaultMcpTransportContext(Collections.unmodifiableMap(new HashMap<>()));

    static McpTransportContext create(Map<String, Object> metadata) {
        return new DefaultMcpTransportContext(metadata);
    }

    Object get(String key);

    class DefaultMcpTransportContext implements McpTransportContext {
        private final Map<String, Object> metadata;

        DefaultMcpTransportContext(Map<String, Object> metadata) {
            Assert.notNull(metadata, "The metadata cannot be null");
            this.metadata = metadata;
        }

        @Override
        public Object get(String key) {
            return this.metadata.get(key);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            DefaultMcpTransportContext that = (DefaultMcpTransportContext) o;
            return this.metadata.equals(that.metadata);
        }

        @Override
        public int hashCode() {
            return this.metadata.hashCode();
        }
    }
}
