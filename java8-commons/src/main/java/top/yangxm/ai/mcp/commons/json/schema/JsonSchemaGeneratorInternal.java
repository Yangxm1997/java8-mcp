package top.yangxm.ai.mcp.commons.json.schema;

import top.yangxm.ai.mcp.commons.util.InterfaceUtils;

final class JsonSchemaGeneratorInternal {
    private JsonSchemaGeneratorInternal() {
    }

    private static JsonSchemaGenerator defaultGenerator = null;

    static JsonSchemaGenerator getDefaultGenerator() {
        if (defaultGenerator == null) {
            defaultGenerator = createDefaultGenerator();
        }
        return defaultGenerator;
    }

    private static JsonSchemaGenerator createDefaultGenerator() {
        return InterfaceUtils.createDefaultInterface(JsonSchemaGenerator.class, JsonSchemaGeneratorSupplier.class);
    }
}
