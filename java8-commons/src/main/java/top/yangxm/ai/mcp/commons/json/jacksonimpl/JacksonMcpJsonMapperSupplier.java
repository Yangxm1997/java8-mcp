package top.yangxm.ai.mcp.commons.json.jacksonimpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.yangxm.ai.mcp.commons.json.McpJsonMapper;
import top.yangxm.ai.mcp.commons.json.McpJsonMapperSupplier;

@SuppressWarnings("unused")
public final class JacksonMcpJsonMapperSupplier implements McpJsonMapperSupplier {

    @Override
    public McpJsonMapper get() {
        return new JacksonMcpJsonMapper(new ObjectMapper());
    }
}
