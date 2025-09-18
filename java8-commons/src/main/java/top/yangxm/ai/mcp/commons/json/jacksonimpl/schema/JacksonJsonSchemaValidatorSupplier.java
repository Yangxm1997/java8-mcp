package top.yangxm.ai.mcp.commons.json.jacksonimpl.schema;

import top.yangxm.ai.mcp.commons.json.schema.JsonSchemaValidator;
import top.yangxm.ai.mcp.commons.json.schema.JsonSchemaValidatorSupplier;

@SuppressWarnings("unused")
public final class JacksonJsonSchemaValidatorSupplier implements JsonSchemaValidatorSupplier {
    @Override
    public JsonSchemaValidator get() {
        return new JacksonJsonSchemaValidator();
    }
}
