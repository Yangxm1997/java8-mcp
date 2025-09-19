package top.yangxm.ai.mcp.commons.json.schema;

public interface JsonSchemaGenerator {
    String generate(Class<?> clazz);

    static JsonSchemaGenerator getDefault() {
        return JsonSchemaGeneratorInternal.getDefaultGenerator();
    }
}
