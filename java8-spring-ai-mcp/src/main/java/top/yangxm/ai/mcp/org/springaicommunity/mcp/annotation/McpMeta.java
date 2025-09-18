package top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation;

import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class McpMeta {
    private final Map<String, Object> meta;

    public McpMeta() {
        this.meta = Collections.emptyMap();
    }

    public McpMeta(@Nullable Map<String, Object> meta) {
        this.meta = meta == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(meta));
    }

    public Map<String, Object> meta() {
        return this.meta;
    }

    public Map<String, Object> getMeta() {
        return this.meta();
    }

    @Override
    public String toString() {
        return "McpMeta{" +
                "meta=" + meta +
                '}';
    }
}
