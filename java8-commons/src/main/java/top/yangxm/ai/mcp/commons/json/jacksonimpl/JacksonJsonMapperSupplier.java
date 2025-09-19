package top.yangxm.ai.mcp.commons.json.jacksonimpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import top.yangxm.ai.mcp.commons.json.JsonMapper;
import top.yangxm.ai.mcp.commons.json.JsonMapperSupplier;

@SuppressWarnings("unused")
public final class JacksonJsonMapperSupplier implements JsonMapperSupplier {

    @Override
    public JsonMapper get() {
        return new JacksonJsonMapper(new ObjectMapper());
    }
}
