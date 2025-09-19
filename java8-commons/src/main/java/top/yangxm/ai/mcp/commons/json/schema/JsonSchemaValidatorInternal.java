package top.yangxm.ai.mcp.commons.json.schema;

import top.yangxm.ai.mcp.commons.util.InterfaceUtils;

final class JsonSchemaValidatorInternal {
    private JsonSchemaValidatorInternal() {
    }

    private static JsonSchemaValidator defaultValidator = null;

    static JsonSchemaValidator getDefaultValidator() {
        if (defaultValidator == null) {
            defaultValidator = createDefaultValidator();
        }
        return defaultValidator;
    }

    private static JsonSchemaValidator createDefaultValidator() {
        return InterfaceUtils.createDefaultInterface(JsonSchemaValidator.class, JsonSchemaValidatorSupplier.class, null);
    }
}
