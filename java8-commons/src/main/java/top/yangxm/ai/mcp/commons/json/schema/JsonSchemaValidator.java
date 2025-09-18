package top.yangxm.ai.mcp.commons.json.schema;

import java.util.Map;

@SuppressWarnings("unused")
public interface JsonSchemaValidator {
    ValidationResponse validate(Map<String, Object> schema, Object structuredContent);

    static JsonSchemaValidator getDefault() {
        return JsonSchemaInternal.getDefaultValidator();
    }
}